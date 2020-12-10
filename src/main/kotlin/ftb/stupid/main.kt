package ftb.stupid

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


const val SUCCESS = 0
const val INVALID_OP = -1
const val FAIL = 1

@ExperimentalTime
fun main(args: Array<String>) {
    when (val op = parseOp(args)) {
        HelpOp -> {
            println("""
                |=====================================================
                |=====================================================
                |FTB Stupid version qw3erfasdf123423211
                |
                |extract "<ftbquests config 폴더>" :
                |   컨피그 내부의 모든 번역 가능한 문자열을 긁어옵니다.
                |   번역 키로 내용이 대체된 퀘스트 파일과 긁어온 문자열 파일은 out 폴더에 저장됩니다.
                |
                |=====================================================
                |=====================================================
            """.trimMargin())
            exitProcess(SUCCESS)
        }
        is ErrorOp -> {
            System.err.println(op.message)
            exitProcess(INVALID_OP)
        }
        is ExtractOp -> {
            exitProcess(
                if (executeOp("extract") {
                        runBlocking {
                            readFromQuest(op.file)
                        }
                    }) SUCCESS
                else FAIL
            )
        }
    }
}

@ExperimentalTime
private inline fun executeOp(opName: String, op: () -> OpResult): Boolean {
    val res: OpResult
    val time = measureTime {
        try {
            res = op()
        } catch (ex: Exception) {
            System.err.println("예상치 못한 오류가 발생했습니다 :")
            ex.printStackTrace()
            exitProcess(FAIL)
        }
    }
    return when (res) {
        is Success -> {
            System.out.println("$opName 작업을 성공적으로 마쳤습니다! 소요된 시간: $time ms.")
            true
        }
        is Fail -> {
            System.err.println("$opName 작업을 끝마치지 못했습니다.")
            System.err.println(" : ${res.message}")
            false
        }
    }
}

private fun parseOp(args: Array<String>): Op {
    if (args.isEmpty()) return HelpOp
    when (args[0]) {
        "extract" -> {
            if (args.size < 2) return ErrorOp("에러 : extract 동작은 폴더 경로를 추가로 필요로 합니다.")
            return ExtractOp(args[1])
        }
        "help" -> return HelpOp
        else -> return ErrorOp("에러 : '${args[0]}'는 지원되지 않는 동작입니다.")
    }
}

sealed class Op

object HelpOp : Op()
class ErrorOp(val message: String) : Op()
class ExtractOp(val file: String) : Op()

sealed class OpResult
object Success : OpResult()
class Fail(val message: String) : OpResult()