package ftb.stupid

import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import ftb.stupid.nbt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class Localizer(private val rootFile: File) {
	val translations = JsonObject()
	var fileCount = 0
		private set

	suspend fun run() = coroutineScope {
		val dataFile = File(rootFile, "data.snbt")
		if(dataFile.isFile) {
			println("data.snbt 파일 확인...")
			fileCount++
			launch {
				readSNBT(dataFile) { nbt -> convertData(nbt) }
			}
		}

		val chapters = File(rootFile, "chapters")
		if(chapters.isDirectory) {
			println("chapters 서브 폴더 확인...")
			for(file in chapters.walk()) {
				if(file.isFile&&file.extension.equals("snbt", true)) {
					fileCount++
					launch {
						readSNBT(file) { nbt -> convertChapters(nbt) }
					}
				}
			}
		}

		val rewardTables = File(rootFile, "reward_tables")
		if(rewardTables.isDirectory) {
			println("reward_tables 서브 폴더 확인...")
			for(file in rewardTables.walk()) {
				if(file.isFile&&file.extension.equals("snbt", true)) {
					fileCount++
					launch {
						readSNBT(file) { nbt -> convertRewardTable(nbt) }
					}
				}
			}
		}
	}

	private fun convertData(nbt: NBTObject) {
		nbt["title"]?.asString?.let {
			translations["quest.title"] = it.value
			nbt["title"] = "{quest.title}"
		}
	}

	private fun convertChapters(nbt: NBTObject) {
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
			for(o in quests.map { it.asObject }) {
				val id = o["id"]!!.asInt.value
				o["title"]?.asString?.let {
					translations["$key.$id.title"] = it.value
					nbt["title"] = "{$key.$id.title}"
				}
				o["description"]?.asList?.let { desc ->
					var i = 0
					for((idx, s) in desc.list.withIndex()) {
						val value = s.asString.value
						if(value.isNotBlank()) {
							translations["$key.$id.desc.$i"] = value
							desc[idx] = "{$key.$id.desc.$i}"
							i++
						}
					}
				}
			}
		}
	}

	private fun convertRewardTable(nbt: NBTObject) {
		val id = nbt["id"]!!.asInt.value
		nbt["title"]?.asString?.let {
			translations["quest.reward.$id.title"] = it.value
			nbt["title"] = "{quest.reward.$id.title}"
		}
	}

	private suspend inline fun readSNBT(file: File, crossinline action: (NBTObject) -> Unit) = coroutineScope {
		val text = file.reader(Charsets.UTF_8).use {
			withContext(Dispatchers.IO) {
				it.readText()
			}
		}
		val nbt = try {
			JsonToNBT.getTagFromJson(text)
		} catch(ex: NBTException) {
			throw RuntimeException("파일 ${file.absolutePath} 로드 중 예상치 못한 오류가 발생했습니다.", ex)
		}
		action(nbt)
		writeGlobalized(file, nbt)
	}

	private suspend fun writeGlobalized(originalFile: File, globalized: NBTObject) = coroutineScope {
		launch {
			val file = File(
					originalFile.parent.replaceLast("quests", "quests\\out"),
					originalFile.name
			)
			withContext(Dispatchers.IO) {
				file.parentFile?.mkdirs()
				file.writeText(nbtToString(globalized), Charsets.UTF_8)
			}
		}
	}
}