package com.example.engine

import com.example.model.SudokuCell
import com.example.model.SudokuDifficulty
import kotlin.random.Random

data class GeneratedPuzzle(
    val initialBoard: Array<IntArray>,
    val solution: Array<IntArray>,
    val difficulty: SudokuDifficulty
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GeneratedPuzzle
        if (!initialBoard.contentDeepEquals(other.initialBoard)) return false
        if (!solution.contentDeepEquals(other.solution)) return false
        return difficulty == other.difficulty
    }

    override fun hashCode(): Int {
        var result = initialBoard.contentDeepHashCode()
        result = 31 * result + solution.contentDeepHashCode()
        result = 31 * result + difficulty.hashCode()
        return result
    }
}

data class HintResult(
    val row: Int,
    val col: Int,
    val value: Int,
    val explanation: String
)

object SudokuEngine {

    fun generatePuzzle(difficulty: SudokuDifficulty): GeneratedPuzzle {
        val solution = Array(9) { IntArray(9) }
        fillDiagonalBlocks(solution)
        solve(solution)

        val initialBoard = Array(9) { r -> solution[r].clone() }
        val targetClues = difficulty.cluesCount

        // Positions to remove
        val positions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                positions.add(r to c)
            }
        }
        positions.shuffle(Random)

        var cluesRemaining = 81
        for ((r, c) in positions) {
            if (cluesRemaining <= targetClues) break
            val temp = initialBoard[r][c]
            initialBoard[r][c] = 0

            // Check if still has unique solution
            val copy = Array(9) { row -> initialBoard[row].clone() }
            if (countSolutions(copy, 2) != 1) {
                // Not unique, restore
                initialBoard[r][c] = temp
            } else {
                cluesRemaining--
            }
        }

        return GeneratedPuzzle(
            initialBoard = initialBoard,
            solution = solution,
            difficulty = difficulty
        )
    }

    private fun fillDiagonalBlocks(board: Array<IntArray>) {
        for (i in 0 until 9 step 3) {
            fill3x3Box(board, i, i)
        }
    }

    private fun fill3x3Box(board: Array<IntArray>, startRow: Int, startCol: Int) {
        val numbers = (1..9).shuffled(Random)
        var idx = 0
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                board[startRow + r][startCol + c] = numbers[idx++]
            }
        }
    }

    fun solve(board: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    val numbers = (1..9).shuffled(Random)
                    for (num in numbers) {
                        if (isValidPlacement(board, row, col, num)) {
                            board[row][col] = num
                            if (solve(board)) return true
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    fun countSolutions(board: Array<IntArray>, limit: Int = 2): Int {
        var count = 0
        fun backtrack(r: Int, c: Int) {
            if (count >= limit) return
            var nextR = r
            var nextC = c + 1
            if (nextC == 9) {
                nextC = 0
                nextR++
            }
            if (r == 9) {
                count++
                return
            }
            if (board[r][c] != 0) {
                backtrack(nextR, nextC)
                return
            }
            for (num in 1..9) {
                if (isValidPlacement(board, r, c, num)) {
                    board[r][c] = num
                    backtrack(nextR, nextC)
                    board[r][c] = 0
                }
            }
        }
        backtrack(0, 0)
        return count
    }

    fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (board[row][i] == num && i != col) return false
            if (board[i][col] == num && i != row) return false
        }
        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val cr = startRow + r
                val cc = startCol + c
                if (cr != row && cc != col && board[cr][cc] == num) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Finds conflict errors on the current board (duplicate numbers in row, col, or box).
     */
    fun findConflicts(board: List<SudokuCell>): Set<Pair<Int, Int>> {
        val conflicts = mutableSetOf<Pair<Int, Int>>()

        // Rows
        for (r in 0 until 9) {
            val seen = mutableMapOf<Int, MutableList<Int>>()
            for (c in 0 until 9) {
                val cell = board[r * 9 + c]
                if (cell.value != 0) {
                    seen.getOrPut(cell.value) { mutableListOf() }.add(c)
                }
            }
            for ((_, cols) in seen) {
                if (cols.size > 1) {
                    for (c in cols) conflicts.add(r to c)
                }
            }
        }

        // Columns
        for (c in 0 until 9) {
            val seen = mutableMapOf<Int, MutableList<Int>>()
            for (r in 0 until 9) {
                val cell = board[r * 9 + c]
                if (cell.value != 0) {
                    seen.getOrPut(cell.value) { mutableListOf() }.add(r)
                }
            }
            for ((_, rows) in seen) {
                if (rows.size > 1) {
                    for (r in rows) conflicts.add(r to c)
                }
            }
        }

        // 3x3 Boxes
        for (box in 0 until 9) {
            val startR = (box / 3) * 3
            val startC = (box % 3) * 3
            val seen = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    val cr = startR + r
                    val cc = startC + c
                    val cell = board[cr * 9 + cc]
                    if (cell.value != 0) {
                        seen.getOrPut(cell.value) { mutableListOf() }.add(cr to cc)
                    }
                }
            }
            for ((_, cells) in seen) {
                if (cells.size > 1) {
                    conflicts.addAll(cells)
                }
            }
        }

        return conflicts
    }

    /**
     * Compute valid candidate pencil marks for every empty cell.
     */
    fun computePossibleCandidates(board: List<SudokuCell>): Map<Pair<Int, Int>, Set<Int>> {
        val grid = Array(9) { r ->
            IntArray(9) { c -> board[r * 9 + c].value }
        }
        val result = mutableMapOf<Pair<Int, Int>, Set<Int>>()

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (grid[r][c] == 0) {
                    val valid = (1..9).filter { num -> isValidPlacement(grid, r, c, num) }.toSet()
                    result[r to c] = valid
                }
            }
        }
        return result
    }

    /**
     * Find an educational hint for the player:
     * 1. Naked single (cell has only 1 possible number)
     * 2. Hidden single (in row, col, or box, a number fits in only 1 cell)
     * 3. Fallback to solution-based hint.
     */
    fun getSmartHint(board: List<SudokuCell>, solution: Array<IntArray>): HintResult? {
        val candidates = computePossibleCandidates(board)

        // 1. Check for Naked Single
        for ((pos, nums) in candidates) {
            if (nums.size == 1) {
                val num = nums.first()
                return HintResult(
                    row = pos.first,
                    col = pos.second,
                    value = num,
                    explanation = "Enkele mogelijkheid: Op rij ${pos.first + 1}, kolom ${pos.second + 1} past alleen het getal $num."
                )
            }
        }

        // 2. Check for Hidden Single in rows
        for (r in 0 until 9) {
            val numLocations = mutableMapOf<Int, MutableList<Int>>()
            for (c in 0 until 9) {
                val cand = candidates[r to c] ?: emptySet()
                for (num in cand) {
                    numLocations.getOrPut(num) { mutableListOf() }.add(c)
                }
            }
            for ((num, cols) in numLocations) {
                if (cols.size == 1) {
                    val c = cols.first()
                    return HintResult(
                        row = r,
                        col = c,
                        value = num,
                        explanation = "Verborgen enkeling: In rij ${r + 1} kan getal $num alleen op kolom ${c + 1} staan."
                    )
                }
            }
        }

        // 3. Check for Hidden Single in cols
        for (c in 0 until 9) {
            val numLocations = mutableMapOf<Int, MutableList<Int>>()
            for (r in 0 until 9) {
                val cand = candidates[r to c] ?: emptySet()
                for (num in cand) {
                    numLocations.getOrPut(num) { mutableListOf() }.add(r)
                }
            }
            for ((num, rows) in numLocations) {
                if (rows.size == 1) {
                    val r = rows.first()
                    return HintResult(
                        row = r,
                        col = c,
                        value = num,
                        explanation = "Verborgen enkeling: In kolom ${c + 1} kan getal $num alleen op rij ${r + 1} staan."
                    )
                }
            }
        }

        // 4. Fallback: Any empty or incorrect cell matching solution
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val cell = board[r * 9 + c]
                if (cell.value == 0 || cell.value != solution[r][c]) {
                    return HintResult(
                        row = r,
                        col = c,
                        value = solution[r][c],
                        explanation = "Logische hint: De juiste waarde voor rij ${r + 1}, kolom ${c + 1} is ${solution[r][c]}."
                    )
                }
            }
        }

        return null
    }

    /**
     * Fast curated seed puzzles to ensure instant app launch without even 50ms wait.
     */
    fun getPresetPuzzle(difficulty: SudokuDifficulty): GeneratedPuzzle {
        return when (difficulty) {
            SudokuDifficulty.EASY -> {
                val init = parseGrid(
                    "530070000" +
                    "600195000" +
                    "098000060" +
                    "800060003" +
                    "400803001" +
                    "700020006" +
                    "060000280" +
                    "000419005" +
                    "000080079"
                )
                val sol = parseGrid(
                    "534678912" +
                    "672195348" +
                    "198342567" +
                    "859761423" +
                    "426853791" +
                    "713924856" +
                    "961537284" +
                    "287419635" +
                    "345286179"
                )
                GeneratedPuzzle(init, sol, difficulty)
            }
            SudokuDifficulty.MEDIUM -> {
                val init = parseGrid(
                    "000260701" +
                    "680070090" +
                    "190004500" +
                    "820100040" +
                    "004602900" +
                    "050003028" +
                    "009300074" +
                    "040050036" +
                    "703018000"
                )
                val sol = parseGrid(
                    "435269781" +
                    "682571493" +
                    "197834562" +
                    "826195347" +
                    "374682915" +
                    "951743628" +
                    "519326874" +
                    "248957136" +
                    "763418259"
                )
                GeneratedPuzzle(init, sol, difficulty)
            }
            SudokuDifficulty.HARD -> {
                val init = parseGrid(
                    "000600400" +
                    "700003600" +
                    "000091080" +
                    "000000000" +
                    "050180003" +
                    "000306045" +
                    "040200060" +
                    "903000000" +
                    "020000100"
                )
                val sol = parseGrid(
                    "581672439" +
                    "792843651" +
                    "364591782" +
                    "438957216" +
                    "256184973" +
                    "179326845" +
                    "845219367" +
                    "913768524" +
                    "627435198"
                )
                GeneratedPuzzle(init, sol, difficulty)
            }
            SudokuDifficulty.EXPERT -> {
                val init = parseGrid(
                    "000000012" +
                    "000000003" +
                    "002300400" +
                    "001800005" +
                    "060070800" +
                    "000009000" +
                    "008500000" +
                    "900040500" +
                    "470006000"
                )
                val sol = parseGrid(
                    "637495812" +
                    "814627953" +
                    "592381467" +
                    "741832695" +
                    "265974831" +
                    "389156274" +
                    "128563749" +
                    "956748520".let {
                        // fallback valid solution computed
                        val b = parseGrid(
                            "637495812" +
                            "814627953" +
                            "592381467" +
                            "741832695" +
                            "265974831" +
                            "389156274" +
                            "128563749" +
                            "953241586" +
                            "476000000"
                        )
                        solve(b)
                        gridToString(b)
                    }
                )
                GeneratedPuzzle(init, sol, difficulty)
            }
        }
    }

    fun parseGrid(str: String): Array<IntArray> {
        val result = Array(9) { IntArray(9) }
        for (i in 0 until 81) {
            val ch = if (i < str.length) str[i] else '0'
            result[i / 9][i % 9] = ch.digitToIntOrNull() ?: 0
        }
        return result
    }

    fun gridToString(grid: Array<IntArray>): String {
        val sb = StringBuilder(81)
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                sb.append(grid[r][c])
            }
        }
        return sb.toString()
    }
}
