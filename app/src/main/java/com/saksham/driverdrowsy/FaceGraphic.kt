package com.saksham.driverdrowsy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.vision.face.Face
import com.saksham.driverdrowsy.camera.GraphicOverlay

class FaceGraphic(mOverlay: GraphicOverlay) : GraphicOverlay.Graphic(mOverlay) {

    private var mFacePositionPaint: Paint
    private var mIdPaint: Paint
    private var mBoxPaint: Paint

    @Volatile
    private lateinit var mFace: Face
    private var mFaceId = 0
    private val mFaceHappiness = 0f

    init {
        mCurrentColorIndex =
            (mCurrentColorIndex + 1) % 1
        val selectedColor: Int = Color.GREEN

        mFacePositionPaint = Paint()
        mFacePositionPaint.color = selectedColor

        mIdPaint = Paint()
        mIdPaint.color = selectedColor
        mIdPaint.textSize = ID_TEXT_SIZE

        mBoxPaint = Paint()
        mBoxPaint.color = selectedColor
        mBoxPaint.style = Paint.Style.STROKE
        mBoxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    fun setId(id: Int) {
        mFaceId = id
    }

    fun updateFace(face: Face) {
        mFace = face
        postInvalidate()
    }

    override fun draw(canvas: Canvas?) {
        if (canvas != null) {
            val face = mFace

            // Draws a circle at the position of the detected face, with the face's track id below.

            // Draws a circle at the position of the detected face, with the face's track id below.
            val x = translateX(face.position.x + face.width / 2)
            val y = translateY(face.position.y + face.height / 2)
            canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint)
//            canvas.drawText("id: $mFaceId", x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint)

            // Draws a bounding box around the face.

            // Draws a bounding box around the face.
            val xOffset = scaleX(face.width / 2.0f)
            val yOffset = scaleY(face.height / 2.0f)
            val left = x - xOffset
            val top = y - yOffset
            val right = x + xOffset
            val bottom = y + yOffset
            canvas.drawRect(left, top, right, bottom, mBoxPaint)
        }
    }

    companion object {
        private val FACE_POSITION_RADIUS = 10.0f
        private val ID_TEXT_SIZE = 40.0f
        private val ID_Y_OFFSET = 50.0f
        private val ID_X_OFFSET = -50.0f
        private val BOX_STROKE_WIDTH = 5.0f
        private var mCurrentColorIndex = 0
    }
}