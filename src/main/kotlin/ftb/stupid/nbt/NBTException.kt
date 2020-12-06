package ftb.stupid.nbt

class NBTException(message: String, json: String, length: Int) :
    Exception(message + " at: " + slice(json, length)) {

    companion object {
        private fun slice(json: String, length: Int): String {
            val stringbuilder = StringBuilder()
            val i = Math.min(json.length, length)
            if (i > 35) {
                stringbuilder.append("...")
            }
            stringbuilder.append(json.substring(Math.max(0, i - 35), i))
            stringbuilder.append("<--[HERE]")
            return stringbuilder.toString()
        }
    }
}