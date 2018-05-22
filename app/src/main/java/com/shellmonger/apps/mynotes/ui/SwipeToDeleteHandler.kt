/*
    Copyright 2018 Adrian Hall

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.shellmonger.apps.mynotes.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.shellmonger.apps.mynotes.R

class SwipeToDeleteHandler(context: Context, private val onDelete: (ListItemViewHolder) -> Unit) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val background = ColorDrawable(Color.RED)
    private val xMark = ContextCompat.getDrawable(context, R.drawable.ic_clear_24dp)?.apply {
        setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
    }
    private val xMarkMargin = context.resources.getDimension(R.dimen.ic_clear_margin).toInt()


    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean
            = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
        viewHolder?.let {
            val listItemViewHolder = viewHolder as ListItemViewHolder
            onDelete(listItemViewHolder)
        }
    }

    override fun onChildDraw(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (viewHolder != null) {
            if (viewHolder.adapterPosition < 0) return

            val view = viewHolder.itemView // the view being swiped

            // draw the red background, based on the offset of the swipe (dX)
            background.apply {
                setBounds(view.right + dX.toInt(), view.top, view.right, view.bottom)
                draw(c)
            }

            // draw the symbol
            xMark?.apply {
                val xt = view.top + (view.bottom - view.top - xMark.intrinsicHeight) / 2
                setBounds(
                        view.right - xMarkMargin - xMark.intrinsicWidth,
                        xt,
                        view.right - xMarkMargin,
                        xt + xMark.intrinsicHeight
                )
                draw(c)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}