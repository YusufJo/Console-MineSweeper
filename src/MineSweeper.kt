package minesweeper

import java.util.*
import kotlin.random.Random

class MineSweeper(private val numOfMines: Int, private val rows: Int, private val columns: Int, private val safeCellSymbol: Char) {
    private val discoveredCellSymbol = '*'
    private val mineCellSymbol = 'X'
    private val freeCellSymbol = '/'
    private val initialCells = CharArray(rows * columns) { safeCellSymbol }
    private var cellsInProgress: CharArray = CharArray(rows * columns) { safeCellSymbol }
    private lateinit var finalCells: CharArray
    private lateinit var indexesOfCellsWithHints: IntArray
    private lateinit var indexesOfMineCells: IntArray
    private lateinit var indexesOfFreeCells: IntArray
    private val scanner = Scanner(System.`in`)
    private val promptMessage = "Set/unset mines marks or claim a cell as free: "
    private val wrongColMessage = "Column is not correct, please try again"
    private val wrongRowMessage = "Row is not correct, please try again"
    private val outOfBoundsMessage = "Index is out of bounds, please select an appropriate index!"
    private val wrongCellStateMessage = "State is not correct, please try again"
    private val winMessage = "Congratulations! You found all mines!"
    private val lossMessage = "You stepped on a mine and failed!"
    private var playerWonTheGame = true

    private enum class UserSelectedCellState { FREE, MINE }

    /**
     * post: numOfMines is greater than zero and less than total cells in the table
     */
    init {
        if (numOfMines >= rows * columns || numOfMines <= 0)
            throw IllegalArgumentException("Number of mines can be only less than total cells and greater than zero")

    }


    /**
     * The entry point of the game, starts by printing a table filled with safeCellSymbol characters, then prompts the player
     * to enter : 1 - a row: an integer representing a row of a cell in the table
     *            2 - a column: an integer representing a column of a cell in the table
     *            3 - a state: a string representing a guess of the state of the cell (either "free" or "mine")
     *
     * After getting the correct input from the player, an action is taken on the selected cell and reflected in cellsInProgress CharArray,
     * the player can mark and unmark all cells as mines at the beginning, but the actual game starts only when the player enters
     * the first cell with a state of "free".
     *
     * To win this game, the player has 2 options: 1 - mark all mines.
     *                                             2 - mark all free cells and digits.
     *
     * The player loses the game when marking a cell that contains a mine as "free" cell.
     */
    fun startGame() {
        printInitialCells()
        var userChosenCell = getInBoundCell()
        // go through this loop until user input a cell state of free
        while (userChosenCell.second != UserSelectedCellState.FREE.name.toLowerCase()) {
            when (cellsInProgress[userChosenCell.first]) {
                // set as discovered
                safeCellSymbol -> cellsInProgress[userChosenCell.first] = discoveredCellSymbol
                // unset discovered
                discoveredCellSymbol -> cellsInProgress[userChosenCell.first] = safeCellSymbol
            }
            printCellsInProgress()
            userChosenCell = getInBoundCell()
        }
        // reset all changes made to cellsInProgress since the actual game didn't start yet
        cellsInProgress = CharArray(rows * columns) { safeCellSymbol }
        // prepare cell arrays after user chose the first free cell
        prepareCellArrays(userChosenCell.first)
        // reveal free cells and hints around first free cell
        markFreeCellsAroundIndexOf(userChosenCell.first)
        printCellsInProgress()
        // this loop breaks when user wins ((discovers all mines) or (mark all free cells and digits) or loses (marks a mine symbol as free)
        mainGameLoop@ while (!allMinesAreDiscovered() && !(allFreeCellsAreMarked() && allCellsWithHintsAreMarked())) {
            // get cell index and state from user
            val selectedCell = getInBoundCell()
            // test if user lost the game
            if (handleUserSelectedCell(selectedCell)) break@mainGameLoop

            printCellsInProgress()
        }
        // print a message at the end of the game
        println(if (playerWonTheGame) winMessage else lossMessage)
    }

    /**
     * react to the user CORRECT input, and reflect the reaction in cellsInProgress
     *
     *  pre: 1 - selectedCell is a valid cell
     *  @see getInBoundCell
     *       2 - finalCells is filled with appropriate data
     *  @see prepareFinalCells
     */
    private fun handleUserSelectedCell(selectedCell: Pair<Int, String>): Boolean {
        // since finalCells has the final answers to the game, we test selected cells against it
        when {
            // if user selects a cell which should hold a number, the number is revealed in cellsInProgress
            finalCells[selectedCell.first].isDigit() -> cellsInProgress[selectedCell.first] = finalCells[selectedCell.first]

            // user selects a cell which has a mine (note that in finalCells a mine is denoted by a discovered cell)
            finalCells[selectedCell.first] == discoveredCellSymbol -> when (selectedCell.second) {
                // user marks/unmark the cell as mine
                UserSelectedCellState.MINE.name.toLowerCase() -> when (cellsInProgress[selectedCell.first]) {
                    // if user marked this cell as mine before, reset it to be safe cell
                    discoveredCellSymbol -> cellsInProgress[selectedCell.first] = safeCellSymbol
                    // if it's a safe cell then mark it as discovered
                    safeCellSymbol -> cellsInProgress[selectedCell.first] = discoveredCellSymbol
                }
                // user marks the cell as free (user loses here)
                UserSelectedCellState.FREE.name.toLowerCase() -> {
                    endGameWithLoss()
                    return true
                }
            }

            // user selects a cell which should be free, the cell and the adjacent free cells are marked free in cellsInProgress
            finalCells[selectedCell.first] == freeCellSymbol -> markFreeCellsAroundIndexOf(selectedCell.first)
        }
        return false
    }

    /**
     * ** Recursive method **
     * If the cell at the given index is empty and has no mines around,
     * all the cells around it, including the marked ones, are marked (as free or denoted by a digit).
     * Also, if next to the marked cell is another empty one with no mines around,
     * all cells around it are explored as well, and so on until none can be explored automatically.
     *
     * pre: finalCells is filled with its appropriate data
     * @see prepareFinalCells
     *
     * @param index is an index of a cell in the game, which the user thinks it's a free cell
     */
    private fun markFreeCellsAroundIndexOf(index: Int) {
        // base case 1 when index is out of bounds, return
        if (index !in 0 until rows * columns) return
        // base case 2 when cell at index is number, reveal number in cellsInProgress
        if (finalCells[index].isDigit()) {
            cellsInProgress[index] = finalCells[index]
            return
        }
        // recursive case
        else if (finalCells[index] == freeCellSymbol && cellsInProgress[index] != freeCellSymbol) {
            // mark cell as free
            cellsInProgress[index] = freeCellSymbol
            // check the north of the cell
            markFreeCellsAroundIndexOf(index - columns)
            // check the south of cell
            markFreeCellsAroundIndexOf(index + columns)
            // check the east of cell if cell is not in the extreme right
            if ((index + 1) % columns != 0) markFreeCellsAroundIndexOf(index + 1)
            // check the west of cell if cell is not in the extreme left
            if (index % columns != 0) markFreeCellsAroundIndexOf(index - 1)
        }
    }

    /**
     * implement the scenario when the user loses the game
     *
     * post: cellsInProgress has all mines shown as mineSymbol (X)
     */
    private fun endGameWithLoss() {
        playerWonTheGame = false
        // show all mines in cellsInProgress
        indexesOfMineCells.forEach { cellsInProgress[it] = mineCellSymbol }
        printCellsInProgress()
    }

    /**
     * handling user input to get only a valid guess representing a cell index and state,
     * the index should be from 0 to (rows * columns) - 1, and the state can be either free or mine (case insensitive)
     *
     * example : Set/unset mines marks or claim a cell as free: 3 2 free
     *
     * pre: scanner should be initialized
     * @return a valid cell index with a state of "free" or "mine"
     */
    private fun getInBoundCell(): Pair<Int, String> {
        val col = scanner.promptUser(promptMessage, wrongColMessage, scanner::nextInt, scanner::hasNextInt)
        val row = scanner.promptUser("", wrongRowMessage, scanner::nextInt, scanner::hasNextInt)
        var cellState = scanner.promptUser("", wrongCellStateMessage, scanner::next, scanner::hasNext)?.toLowerCase()
        var correctIndex = (col - 1) + ((row - 1) * columns)
        // validate state is either free or mine
        while (cellState != UserSelectedCellState.FREE.name.toLowerCase() && cellState != UserSelectedCellState.MINE.name.toLowerCase()) {
            println(wrongCellStateMessage)
            print("Enter a cell state: ")
            cellState = scanner.promptUser("", wrongCellStateMessage, scanner::next, scanner::hasNext)?.toLowerCase()
        }
        // validate index in bounds
        while (correctIndex !in 0 until rows * columns) {
            println(outOfBoundsMessage)
            val colAgain = scanner.promptUser(promptMessage, wrongColMessage, scanner::nextInt, scanner::hasNextInt)
            val rowAgain = scanner.promptUser("", wrongRowMessage, scanner::nextInt, scanner::hasNextInt)
            correctIndex = (colAgain - 1) + ((rowAgain - 1) * columns)
        }
        return Pair(correctIndex, cellState)
    }

    /**
     * a check on the progress of the player
     *
     * pre: indexesOfCellsWithHints is initialized with the appropriate data
     *
     * @return true     when all cells holding digits are revealed in cellsInProgress
     *         false    when at least one cell holding a digit is not revealed in cellsInProgress
     */
    private fun allCellsWithHintsAreMarked(): Boolean {
        indexesOfCellsWithHints.forEach { if (!cellsInProgress[it].isDigit()) return false }
        // all digits are revealed
        return true
    }

    /**
     * a check on the progress of the player
     *
     * pre: indexesOfMineCells is initialized with the appropriate data
     *
     * @return true     when all cells holding mines are marked as discovered in cellsInProgress
     *         false    when at least one cell holding a mine is not marked as discovered in cellsInProgress
     */
    private fun allMinesAreDiscovered(): Boolean {
        indexesOfMineCells.forEach { if (cellsInProgress[it] != discoveredCellSymbol) return false }
        // all mine cells are discovered
        return true
    }

    /**
     * a check on the progress of the player
     *
     * pre: indexesOfFreeCells is initialized with the appropriate data
     *
     * @return true     when all free cells are marked as free in cellsInProgress
     *         false    when at least one free cell is not marked as free in cellsInProgress
     */
    private fun allFreeCellsAreMarked(): Boolean {
        indexesOfFreeCells.forEach { if (cellsInProgress[it] != freeCellSymbol) return false }
        // all free cells are marked free
        return true
    }

    /**
     * print finalCells to the console in table form preceded by an empty line (for testing purposes, to see final results)
     */
    private fun printFinalCells() {
        println()
        wrapInTable(finalCells)
    }

    /**
     * print cellsInProgress to the console in table form preceded by an empty line
     */
    private fun printCellsInProgress() {
        println()
        wrapInTable(cellsInProgress)
    }

    /**
     * print initialCells to the console in table form before making any changes to initialCells array (initialCells has only safe cells)
     *
     * pre: initialCells is not modified since its initialization
     */
    private fun printInitialCells() {
        wrapInTable(initialCells)
    }

    /**
     * fill cell arrays with appropriate data for the first time, these arrays are :
     * @see initialCells
     * @see finalCells
     *
     * @param firstFreeCellIndex the index of the first cell the user mark as free
     */
    private fun prepareCellArrays(firstFreeCellIndex: Int) {
        prepareInitialCells(firstFreeCellIndex)
        prepareFinalCells()
    }

    /**
     * prepare the result of taking the right actions on every cell, the result includes:
     *      1 - marking all mines as discovered
     *      2 - denote every mine-adjacent cell by a digit
     *      3 - marking the rest of safe cells as free cells (cells which are not mines or digits)
     *
     * pre: 1 - prepare initialCells
     *  @see prepareInitialCells
     *
     * post: 1 - indexesOfCellsWithHints is populated with indexes of cells which are digits
     *       2 - finalCells is populated with digits (hints), discovered cells (previously mines) and free cells (the rest of cells)
     *       3 - indexesOfMineCells is populated with indexes which represent discovered cells (previously mines)
     *       4 - indexesOfFreeCells is populated with indexes which represent free cells
     */
    private fun prepareFinalCells() {
        // get indexes of cells which hold numbers
        indexesOfCellsWithHints = initialCells.indices.filter { isAdjacentToMine(it) && initialCells[it] == safeCellSymbol }.toIntArray()
        // copy initialCells to final cells
        finalCells = initialCells.copyOf()
        // replace safe cells which are adjacent to mines with the number of mines they are adjacent to
        indexesOfCellsWithHints.forEach { finalCells[it] = numberOfMinesAroundCell(it).toString().first() }
        // at the end, every mine should be marked discovered and every safe cell should be marked free
        finalCells.forEachIndexed { index, c ->
            when (c) {
                mineCellSymbol -> finalCells[index] = discoveredCellSymbol
                safeCellSymbol -> finalCells[index] = freeCellSymbol
            }
        }
        // here we get the indexes of all mine cells, note that at the end all mines should be discovered
        indexesOfMineCells = finalCells.indices.filter { finalCells[it] == discoveredCellSymbol }.toIntArray()
        // here we get the indexes of all free cells
        indexesOfFreeCells = finalCells.indices.filter { finalCells[it] == freeCellSymbol }.toIntArray()
    }

    /**
     * fill the global initialCells CharArray with mineCellSymbol at random indexes, mark one cell as freeCellSymbol and the
     * rest of the cells should stay safeCellSymbols
     *
     * pre: initialCells should be initialized and filled with safeCellSymbol characters
     * post: initialCells has: 1 - (numOfMines) x mines at random indexes
     *                         2 - (1) x free cell that's not adjacent to a mine
     *                         3 - (totalCells - numOfMines - 1) x safe cells (later on some of them will be digits)
     *
     * @param firstFreeCellIndex the index of the first cell the user mark as free
     */
    private fun prepareInitialCells(firstFreeCellIndex: Int) {
        // number of mines to put in initialCells
        var minesLeft = numOfMines
        // random index of a mine
        var randomIndex: Int
        // keep adding mines to initialCells until minesLeft == zero
        while (minesLeft > 0) {
            // keep on changing the randomIndex of mine if user chose that specific index to be a free cell
            do {
                randomIndex = (0 until rows * columns).shuffled(Random).first()
            } while (randomIndex == firstFreeCellIndex)
            // if the cell at randomIndex is not already a mine, change it from safeCellSymbol to mineCellSymbol
            if (initialCells[randomIndex] != mineCellSymbol) {
                initialCells[randomIndex] = mineCellSymbol
                minesLeft--
            }
            // after adding a mine, make sure it's not adjacent to the initialCells[initialCellIndex], other wise
            // initialCells[initialCellIndex] later will be overridden with a number and we don't want that to happen
            // since initialCells[initialCellIndex] should stay free cell
            if (isAdjacentToMine(firstFreeCellIndex)) {
                initialCells[randomIndex] = safeCellSymbol
                minesLeft++
            }
        }
        // it's now the time to mark initialCells[initialCellIndex] as freeCellSymbol
        initialCells[firstFreeCellIndex] = freeCellSymbol
    }

    /**
     * prints the number of columns then a bar then the actual table then a bar
     * @param cells     the array to be printed to the console
     */
    private fun wrapInTable(cells: CharArray) {
        // like  |12345|
        fun printHeader() {
            repeat(columns.toString().count()) { print(" ") }
            print("|")
            for (i in 1..columns) {
                print(i)
            }
            println('|')
        }

        // like —|—————|
        fun printBar() {
            repeat(columns.toString().count()) { print("—") }
            print("│")
            for (i in 1..columns) {
                repeat(i.toString().count()) { print('—') }
            }
            println('|')
        }

        printHeader()
        printBar()
        printCellsInGrid(cells, columns)
        printBar()
    }

    /**
     * recursive method to print to the console a one dimensional array in the form of a table (rows * columns)
     *
     * pre: 1 - rowCells cannot be zero.
     *      2 - index must be 1 when calling the method.
     *
     * @param cells     the array to be printed to the console
     * @param rowCells  the number of cells in a single row (which is the number of columns in the table)
     * @param index     the index of the current row being printed
     *
     */
    private fun printCellsInGrid(cells: CharArray, rowCells: Int, index: Int = 1) {
        if (cells.isNotEmpty()) {
            println("%${columns.toString().count()}d|${cells.take(rowCells).joinToString(separator = "")}|".format(index))
            printCellsInGrid(cells.drop(rowCells).toCharArray(), rowCells, index + 1)
        }
    }

    /**
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has mines around it
     *         false    when initialCells[index] has no mines around it
     */
    private fun isAdjacentToMine(index: Int): Boolean = numberOfMinesAroundCell(index) > 0

    /**
     * @param index represents the index of a cell in initialCells array
     * @return number of mines around cell at initialCells[index]
     */
    private fun numberOfMinesAroundCell(index: Int): Int {
        return arrayOf(
                containsMineInNorthOf(index),
                containsMineInSouthOf(index),
                containsMineInEastOf(index),
                containsMineInWestOf(index),
                containsMineInNorthEastOf(index),
                containsMineInNorthWestOf(index),
                containsMineInSouthEastOf(index),
                containsMineInSouthWestOf(index)
        ).filter { it }.count()
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the north
     *         false    when initialCells[index] has no north
     *         false    when initialCells[index] has no mine in the east
     */
    private fun containsMineInNorthOf(index: Int): Boolean {
        // cell has no north
        if (index - columns < 0) return false
        return initialCells[index - columns] == mineCellSymbol
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the south
     *         false    when initialCells[index] has no south
     *         false    when initialCells[index] has no mine in the south
     */
    private fun containsMineInSouthOf(index: Int): Boolean {
        // cell has no south
        if (index + columns >= rows * columns) return false
        return initialCells[index + columns] == mineCellSymbol
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the east
     *         false    when index is not in bounds of initialCells
     *         false    when initialCells[index] has no east
     *         false    when initialCells[index] has no mine in the east
     */
    private fun containsMineInEastOf(index: Int): Boolean {
        // cell is not out of bounds
        if ((index + 1) >= rows * columns) return false
        // cell is to the extreme right
        if ((index + 1) % columns == 0) return false
        return initialCells[index + 1] == mineCellSymbol
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the west
     *         false    when initialCells[index] has no west
     *         false    when initialCells[index] has no mine in the west
     */
    private fun containsMineInWestOf(index: Int): Boolean {
        // cell is to the extreme left
        if (index % columns == 0) return false
        return initialCells[index - 1] == mineCellSymbol
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the north east
     *         false    when initialCells[index] has no north
     *         false    when initialCells[index] has no east
     *         false    when initialCells[index] has no mine in the north east
     */
    private fun containsMineInNorthEastOf(index: Int): Boolean {
        return when {
            // cell has no north
            index - columns < 0 -> false
            // cell has no east
            (index + 1) % columns == 0 -> false
            else -> initialCells[index - columns + 1] == mineCellSymbol
        }
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the north west
     *         false    when initialCells[index] has no north
     *         false    when initialCells[index] has no west
     *         false    when initialCells[index] has no mine in the north west
     */
    private fun containsMineInNorthWestOf(index: Int): Boolean {
        return when {
            // cell has no north
            index - columns < 0 -> false
            // cell has no west
            index % columns == 0 -> false
            else -> initialCells[index - columns - 1] == mineCellSymbol
        }
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the south east
     *         false    when initialCells[index] has no south
     *         false    when initialCells[index] has no east
     *         false    when initialCells[index] has no mine in the south east
     */
    private fun containsMineInSouthEastOf(index: Int): Boolean {
        return when {
            // cell has no south
            index + columns >= rows * columns -> false
            // cell has no east
            index == index % (columns - 1) * columns + columns - 1 -> false
            else -> initialCells[index + columns + 1] == mineCellSymbol
        }
    }

    /**
     * pre: initialCells must be initialized (with mines and safe cells)
     * @param index represents the index of a cell in initialCells array
     * @return true     when initialCells[index] has a mine in the south west
     *         false    when initialCells[index] has no south
     *         false    when initialCells[index] has no west
     *         false    when initialCells[index] has no mine in the south west
     */
    private fun containsMineInSouthWestOf(index: Int): Boolean {
        return when {
            // cell has no south
            index + columns >= rows * columns -> false
            // cell has no west
            index % columns == 0 -> false
            else -> initialCells[index + columns - 1] == mineCellSymbol
        }
    }
}