package ftb.stupid

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.text.Charsets.UTF_8


val gson = GsonBuilder()
		.setPrettyPrinting()
		.setLenient()
		.create()

suspend fun readFromQuest(root: String): OpResult = coroutineScope {
	val rootFile = findRootFile(root) ?: return@coroutineScope Fail("${root}는 제대로 된 퀘스트 폴더가 아닌 것 같습니다.")
	if(!rootFile.exists()) return@coroutineScope Fail("폴더 \"${rootFile.absolutePath}\"가 존재하지 않습니다.")
	if(!rootFile.isDirectory) return@coroutineScope Fail("파일 \"${rootFile.absolutePath}\"는 디렉토리가 아닙니다.")

	println("폴더 ${rootFile.absolutePath}를 상대로 추출 작업을 시작합니다...")

	val localizer = Localizer(rootFile)
	localizer.run()

	withContext(Dispatchers.IO) {
		val translationOut = File(rootFile, "out/translation.json")
		translationOut.parentFile?.mkdirs()
		translationOut.writer(UTF_8).use {
			gson.toJson(localizer.translations, it)
		}
	}

	println("${localizer.translations.size()}개의 번역 값 추출. ${localizer.fileCount}개의 퀘스트 파일 복사.")

	return@coroutineScope Success
}

private fun findRootFile(root: String): File? =
		File(root).let {
			when(it.name) {
				"quests" -> it
				"ftbquests" -> File(it, "quests")
				"config" -> File(it, "ftbquests/quests")
				else -> null
			}
		}

/**
 * Wtf kotlin, why do you not have this
 */
fun String.replaceLast(match: String, replaceTo: String): String {
	val index = this.lastIndexOf(match)
	return if(index==-1) this else this.replaceRange(index, index+match.length, replaceTo)
}