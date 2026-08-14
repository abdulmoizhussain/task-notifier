package io.github.abdulmoizhussain.tasknotifier.common

object Console {
    private const val PREFIX = "PREFIX:"

    @Suppress("unused")
    fun log(int: Int) {
        println("${PREFIX}${int}")
    }

    @Suppress("unused")
    fun log(string: String) {
        println("${PREFIX}${string}")
    }
}