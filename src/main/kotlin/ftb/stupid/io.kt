package ftb.stupid

import com.github.salomonbrys.kotson.set
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import ftb.stupid.nbt.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.text.Charsets.UTF_8


val gson = GsonBuilder()
    .setPrettyPrinting()
    .setLenient()
    .create()

suspend fun readFromQuest(root: String): OpResult = coroutineScope {
    val rootFile = findRootFile(root) ?: return@coroutineScope Fail("${root}는 제대로 된 퀘스트 폴더가 아닌 것 같습니다.")
    if (!rootFile.exists()) return@coroutineScope Fail("폴더 \"${rootFile.absolutePath}\"가 존재하지 않습니다.")
    if (!rootFile.isDirectory) return@coroutineScope Fail("파일 \"${rootFile.absolutePath}\"는 디렉토리가 아닙니다.")

    println("폴더 ${rootFile.absolutePath}를 상대로 추출 작업을 시작합니다...")

    val translations = JsonObject()
    val fileCounter = AtomicInteger()

    coroutineScope {
        val dataFile = File(rootFile, "data.snbt")
        if (dataFile.isFile) {
            println("data.snbt 파일 확인...")
            fileCounter.incrementAndGet()
            launch {
                readSNBT(dataFile) { nbt ->
                    dataSnbt(nbt, translations)
                }
            }
        }

        val chapters = File(rootFile, "chapters")
        if (chapters.isDirectory) {
            println("chapters 서브 폴더 확인...")
            for (file in chapters.walk()) {
                if (file.isFile && file.extension.equals("snbt", true)) {
                    fileCounter.incrementAndGet()
                    launch {
                        readSNBT(file) { nbt ->
                           chaptersSnbt(nbt, translations)
                        }
                    }
                }
            }
        }

        val rewardTables = File(rootFile, "reward_tables")
        if (rewardTables.isDirectory) {
            println("reward_tables 서브 폴더 확인...")
            for (file in rewardTables.walk()) {
                if (file.isFile && file.extension.equals("snbt", true)) {
                    fileCounter.incrementAndGet()
                    launch {
                        readSNBT(file) { nbt ->
                            rewardTableSnbt(nbt, translations)
                        }
                    }
                }
            }
        }
    }

    withContext(Dispatchers.IO) {
        val translationOut = File(rootFile, "out/translation.json")
        translationOut.parentFile?.mkdirs()
        translationOut.writer(UTF_8).use {
            gson.toJson(translations, it)
        }
    }

    println("${translations.size()}개의 번역 값 추출. ${fileCounter.get()}개의 퀘스트 파일 복사.")

    return@coroutineScope Success
}

private fun dataSnbt(nbt: NBTObject, translations: JsonObject) {
    nbt["title"]?.asString?.let {
        translations["quest.title"] = it.value
        nbt["title"] = "{quest.title}"
    }
}

private fun chaptersSnbt(nbt: NBTObject, translations: JsonObject){
    val filename = nbt["filename"]!!.asString.value
    val key = "quest.chapter.$filename"

    nbt["title"]?.asString?.let {
        translations["$key.title"] = it.value
        nbt["title"] = "{$key.title}"
    }

    nbt["subtitle"]?.asString?.let {
        translations["$key.subtitle"] = it.value
        nbt["subtitle"] = "{$key.subtitle}"
    }

    nbt["quests"]?.asList?.let { quests ->
        for (o in quests.map { it.asObject }) {
            val id = o["id"]!!.asInt.value
            o["title"]?.asString?.let {
                translations["$key.$id.title"] = it.value
                nbt["title"] = "{$key.$id.title}"
            }
            o["description"]?.asList?.let { desc ->
                var i = 0
                for ((idx, s) in desc.list.withIndex()) {
                    val value = s.asString.value
                    if (value.isNotBlank()) {
                        translations["$key.$id.desc.$i"] = value
                        desc[idx] = "{$key.$id.desc.$i}"
                        i++
                    }
                }
            }
        }
    }
}

private fun rewardTableSnbt(nbt: NBTObject, translations: JsonObject){
    val id = nbt["id"]!!.asInt.value
    nbt["title"]?.asString?.let {
        translations["quest.reward.$id.title"] = it.value
        nbt["title"] = "{quest.reward.$id.title}"
    }
}

private fun findRootFile(root: String): File? =
    File(root).let {
        when (it.name) {
            "quests" -> it
            "ftbquests" -> File(it, "quests")
            "config" -> File(it, "ftbquests/quests")
            else -> null
        }
    }

private suspend inline fun readSNBT(file: File, crossinline action: (NBTObject) -> Unit) {
    coroutineScope {
        val text = file.reader(UTF_8).use {
            withContext(Dispatchers.IO) {
                it.readText()
            }
        }
        val nbt = try {
            JsonToNBT.getTagFromJson(text)
        }catch(ex: NBTException){
            throw RuntimeException("파일 ${file.absolutePath} 로드 중 예상치 못한 오류가 발생했습니다.", ex)
        }
        action(nbt)
        writeGlobalized(file, nbt)
    }
}

private suspend fun writeGlobalized(originalFile: File, globalized: NBTObject) {
    coroutineScope {
        launch {
            val file = File(
                originalFile.parent.replaceLast("quests", "quests\\out"),
                originalFile.name
            )
            withContext(Dispatchers.IO) {
                file.parentFile?.mkdirs()
                file.writeText(nbtToString(globalized), UTF_8)
            }
        }
    }
}

/**
 * Wtf kotlin, why do you not have this
 */
fun String.replaceLast(match: String, replaceTo: String): String {
    val index = this.lastIndexOf(match)
    return if (index == -1) this else this.replaceRange(index, index + match.length, replaceTo)
}