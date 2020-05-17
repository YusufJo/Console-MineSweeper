# Console-MineSweeper - A JetBrains academy project.

The goal of Minesweeper is to uncover all the cells on a grid that do not contain mines
without being "blown up" by marking a cell with a mine as a free cell. 
The location of the mines is discovered through a logical process (but that sometimes results in ambiguity).
Choosing a specific cell will reveal what is hidden underneath it 
(a large number of free cells [bordering 0 mines] may be revealed in one go if they are adjacent to each other).
Some cells are free while others contain numbers (from 1 to 8), with each number being the number of mines adjacent
to the uncovered cell.

To help the player avoid hitting a mine, the location of a suspected mine can be marked by flagging it as mine.
The game is won in two cases: 

1. All free and numbered cells have been uncovered by the player without hitting a mine.
2. All mines have been flagged as mine.

(This description is extracted from https://en.wikipedia.org/wiki/Microsoft_Minesweeper and modified).

### Steps of gameplay
1. Player enters number of mines. (i.e. 8)
2. Player enters a cell number with a desired flag in the format of: "row column flag". (i.e 2 1 free) (i.e 9 1 mine)
* Note: flags can be either "free" or "mine"

### Example of a gameplay

![Alt Text](https://media.giphy.com/media/l03urj1OE7I7zNcYe5/source.gif)

(video by JetBrains Academy https://hyperskill.org/projects/8?goal=347)

 For more about The minesweeper game, check -> https://en.wikipedia.org/wiki/Minesweeper_(video_game).

Thanks, [JetBrains Academy](https://hi.hyperskill.org) :heart:
