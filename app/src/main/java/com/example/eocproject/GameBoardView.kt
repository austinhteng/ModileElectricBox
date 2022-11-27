package com.example.eocproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.view.drawToBitmap

open class GameBoardView(context: Context, val rows: Int, val cols: Int, val viewModel: GameViewModel) :
    View(context) {
    internal var grid: ArrayList<ArrayList<Item>> = ArrayList()
    internal var wireGrid: ArrayList<ArrayList<Pair<Boolean, Boolean>>> = ArrayList() //wire, powered
    private var border: Paint
    private var borderWidth: Int = 0
    protected var cellSz: Float = 0F
    private var backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    init {
        border = Paint(Color.BLACK)
        borderWidth = 5


        for (i in 0 until rows) {
            val nextRow = java.util.ArrayList<Item>()
            val nextWireRow = ArrayList<Pair<Boolean, Boolean>>()
            for (j in 0 until cols) {
                nextRow.add(Item(Item.ItemType.EMPTY, Item.Direction.UP, context))
                nextWireRow.add(Pair(false, false))
            }
            grid.add(nextRow)
            wireGrid.add(nextWireRow)
        }

        this.setOnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) {
                val x = (event.x / cellSz).toInt()
                val y = (event.y / cellSz).toInt()

                val data = event.localState as Item
                val itemType = data.type
                val direction = data.direction
                Log.d("XXX", String.format("Drop received %d %d", x, y))
                Log.d("xxx", itemType.toString())

                if (itemType != Item.ItemType.CABLE) {
                    //Check if empty or replacing another item
                    if (!viewModel.getCreative()) {
                        if (grid[x][y].type != Item.ItemType.EMPTY) {
                            grid[x][y].visibility = VISIBLE
                        }
                        data.visibility = INVISIBLE
                    }
                    grid[x][y] = data
                } else {
                    wireGrid[x][y] = Pair(true, false)
                }

                invalidate()
            }
            true
        }

        this.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = (event.x / cellSz).toInt()
                val y = (event.y / cellSz).toInt()

                val itemType = grid[x][y].type
                val direction = grid[x][y].direction

                if (viewModel.getClearMode()) {
                    if (!viewModel.getCreative()) {
                        if (grid[x][y].type != Item.ItemType.EMPTY) {
                            grid[x][y].visibility = VISIBLE
                        }
                    }
                    if (grid[x][y].type != Item.ItemType.EMPTY) {
                        grid[x][y] = Item(Item.ItemType.EMPTY, Item.Direction.UP, context)
                        invalidate()
                    } else {
                        if (wireGrid[x][y].first) {
                            wireGrid[x][y] = Pair(false, false)
                            invalidate()
                        }
                    }
                } else {    //Rotation mode
                    if (grid[x][y].type != Item.ItemType.EMPTY) {
                        if (viewModel.getCreative()) {
                            when (grid[x][y].direction) {
                                Item.Direction.UP -> grid[x][y].direction = Item.Direction.RIGHT
                                Item.Direction.DOWN -> grid[x][y].direction = Item.Direction.LEFT
                                Item.Direction.LEFT -> grid[x][y].direction = Item.Direction.UP
                                Item.Direction.RIGHT -> grid[x][y].direction = Item.Direction.DOWN
                            }
                        } else {
                            when (grid[x][y].direction) {
                                Item.Direction.UP -> grid[x][y].direction = Item.Direction.DOWN
                                Item.Direction.DOWN -> grid[x][y].direction = Item.Direction.UP
                                Item.Direction.LEFT -> grid[x][y].direction = Item.Direction.RIGHT
                                Item.Direction.RIGHT -> grid[x][y].direction = Item.Direction.LEFT
                            }
                        }
                        invalidate()
                    }
                }
            }
            true
        }

        invalidate()
        Log.d("XXX", "init Gameboard")
    }

    private fun drawCell(canvas: Canvas, paint: Paint, x: Int, y: Int) {
        // XXX Draw a cell at the right location, which is a bordered square
        drawBorderedSquare(canvas, paint, x * cellSz, y * cellSz, cellSz)

        val item = grid[x][y]
        val type = item.type
        val direction = item.direction

        if (wireGrid[x][y].first) {
            val wireColor = if (wireGrid[x][y].second) Paint().apply {color = Color.GREEN} else Paint().apply {color = Color.BLUE}
            drawWire(canvas, x, y, wireColor) //Different color if powered
        }

        when (type) {
            Item.ItemType.EMPTY -> {
            }
            else -> {
                val bitmap = item.drawToBitmap()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, cellSz.toInt(), cellSz.toInt(), true)
                val rotatedBitmap = when (direction) {
                    Item.Direction.LEFT -> {RotateBitmap(scaledBitmap, 270f)}
                    Item.Direction.UP -> {scaledBitmap}
                    Item.Direction.RIGHT -> {RotateBitmap(scaledBitmap, 90f)}
                    Item.Direction.DOWN -> {RotateBitmap(scaledBitmap, 180f)}
                }
                val itemColor = if (wireGrid[x][y].second) Paint().apply { color = Color.YELLOW } else border
                canvas.drawBitmap(rotatedBitmap, x * cellSz, y * cellSz, itemColor)}
        }
    }

    fun RotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun drawBorderedSquare(canvas: Canvas, paint: Paint, i: Float, j: Float, size: Float) {
        // XXX Draw a bordered square
        drawSquare(canvas, paint, i, j, size)
        canvas.drawRect(i, j, i + size, j + borderWidth, border) // top
        canvas.drawRect(i + size - borderWidth, j, i + size, j + size, border) // right
        canvas.drawRect(i, j + size - borderWidth, i + size, j + size, border) // bot
        canvas.drawRect(i, j, i + borderWidth, j + size, border) // left
    }

    private fun drawSquare(canvas: Canvas, paint: Paint?, i: Float, j: Float, size: Float) {
        // XXX Draw a square
        var squareColor = if (paint == null) Paint().apply { color = Color.WHITE } else paint
        canvas.drawRect(i, j, i + size, j + size, squareColor)
    }

    private fun drawWire(canvas: Canvas, x: Int, y: Int, color: Paint) {
        val outletSize = cellSz / 3
        val wireSize = cellSz / 7
        val outletOffset = (cellSz - outletSize) / 2
        val wireOffset = (cellSz - wireSize) / 2

        val cellLeftWall = x * cellSz
        val cellTopWall = y * cellSz
        var adjCount = 0

        if (x > 0 && wireGrid[x-1][y].first) {
            canvas.drawRect(cellLeftWall, cellTopWall + wireOffset,
                cellLeftWall + (cellSz / 2) + wireSize / 2, cellTopWall + cellSz - wireOffset, color)
            adjCount++
        }
        if (x < wireGrid.size-1 && wireGrid[x+1][y].first) {
            canvas.drawRect(cellLeftWall + (cellSz / 2) - wireSize / 2, cellTopWall + wireOffset,
                cellLeftWall + cellSz, cellTopWall + cellSz - wireOffset, color)
            adjCount++
        }
        if (y > 0 && wireGrid[x][y-1].first) {
            canvas.drawRect(cellLeftWall + wireOffset, cellTopWall,
                cellLeftWall + cellSz - wireOffset, cellTopWall + (cellSz / 2) + wireSize / 2, color)
            adjCount++
        }
        if (y < wireGrid[x].size-1 && wireGrid[x][y+1].first) {
            canvas.drawRect(cellLeftWall + wireOffset, cellTopWall + (cellSz / 2) - wireSize / 2,
                cellLeftWall + cellSz - wireOffset, cellTopWall + cellSz, color)
            adjCount++
        }

        if (adjCount < 2) {
            canvas.drawRect(
                cellLeftWall + outletOffset, cellTopWall + outletOffset,
                cellLeftWall + cellSz - outletOffset, cellTopWall + cellSz - outletOffset, color
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldh: Int, oldw: Int) {
        cellSz = if (h >= w)
            w.toFloat() / cols.toFloat()
        else
            h.toFloat() / rows.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundPaint.color = Color.LTGRAY
        canvas.drawPaint(backgroundPaint)

        for (i in 0 until grid.size) {
            for (j in 0 until grid[0].size) {
                drawCell(canvas, backgroundPaint, i, j)
            }
        }
    }
}