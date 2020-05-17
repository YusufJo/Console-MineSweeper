package minesweeper

import java.util.*

private const val howManyMinesMessage = "How many mines do you want on the field?"
private const val errorMessage = "Input is not correct, please try again . . ."

fun main() {
    val scanner = Scanner(System.`in`)
    val numOfMines = scanner.promptUser(howManyMinesMessage, errorMessage, scanner::nextInt, scanner::hasNextInt)
    val mineSweeper = MineSweeper(numOfMines, 9, 9, '.')
    mineSweeper.startGame()
}

fun <T> Scanner.promptUser(promptMessage: String, errorMessage: String, readingFun: () -> T, validatingFun: () -> Boolean): T {
    print("$promptMessage ")
    while (!validatingFun()) {
        println(errorMessage)
        print("$promptMessage ")
        this.next()
    }
    return readingFun()
}