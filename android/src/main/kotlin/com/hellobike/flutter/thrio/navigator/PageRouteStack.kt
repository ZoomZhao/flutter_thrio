// The MIT License (MIT)
//
// Copyright (c) 2019 Hellobike Group
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package com.hellobike.flutter.thrio.navigator

import android.app.Activity
import android.util.ArrayMap
import android.util.Log

internal object PageRouteStack {

    private const val RECORD_INDEX_START = 1
    private const val RECORD_INDEX_STEP = 1

    // 按页面分组记录
    private val subStack by lazy { ArrayMap<Long, MutableList<PageRoute>>() }
    // 记录最后index
    private val lastIndexes by lazy { ArrayMap<String, Int>() }

    // 添加新页
    fun pushNewStack(): Long {
        return System.currentTimeMillis().also {
            subStack[it] = mutableListOf()
        }
    }

    // 移除顶部key对应所有记录
    fun popStack(key: Long) {
        val subStack = subStack
        require(subStack.isNotEmpty()) { "stacks $subStack must not be empty" }
        require(key == subStack.keys.last()) { "only allow pop last key in stack" }
        val stack = subStack.remove(key)
        requireNotNull(stack) { "didn't find stack by this key $key" }
        repeat(stack.size) { pop(stack) }
    }

    fun getKey(record: PageRoute): Long {
        val subStack = subStack
        for (it in subStack.size - 1 downTo 0 step 1) {
            val stack = subStack.valueAt(it)
            if (stack.isEmpty()) {
                continue
            }
            stack.lastOrNull {
                it.url == record.url && it.index == record.index
            } ?: continue
            return subStack.keyAt(it)
        }
        throw IllegalArgumentException("no match key")
    }

    // 只能在最后添加记录
    fun push(key: Long, url: String, clazz: Class<out Activity>): PageRoute {
        val subStack = subStack
        require(key == subStack.keys.last()) { "only the last key allow to push" }
        val stack = subStack[key]
        requireNotNull(stack) { "didn't find stack by this key $key" }
        return push(stack, url, clazz)
    }

    // 移除顶部记录
    fun pop(record: PageRoute) {
        val subStack = subStack
        require(subStack.isNotEmpty()) { "stacks $subStack must not be empty" }
        val key = subStack.keys.last()
        val stack = subStack[key]
        requireNotNull(stack) { "didn't find stack by this key $key" }
        require(stack.isNotEmpty()) { "stack $stack must not be empty" }
        require(stack.last() == record) { "only allow pop last record in stack" }
        pop(stack)
        if (stack.isEmpty()) {
            subStack.remove(key)
        }
    }


    // 移除到指定记录
    fun popTo(record: PageRoute) {
        val subStack = subStack
        require(subStack.isNotEmpty()) { "stacks $subStack must not be empty" }
        repeat(subStack.size) {
            val key = subStack.keys.last()
            val stack = subStack.values.last()
            repeat(stack.size) {
                val last = stack.last()
                if (record == last) {
                    return
                }
                pop(stack)
                if (stack.isEmpty()) {
                    subStack.remove(key)
                }
            }
        }
        throw IllegalArgumentException("no match record")
    }

    private fun push(stack: MutableList<PageRoute>, url: String, clazz: Class<out Activity>): PageRoute {
        val index = lastIndexes[url]?.plus(RECORD_INDEX_STEP) ?: RECORD_INDEX_START
        val record = PageRoute(url, index, clazz)
        stack.add(record)
        lastIndexes[record.url] = record.index
        Log.e("Thrio", "stack push url ${record.url} index ${record.index}")
        return record
    }

    private fun pop(stack: MutableList<PageRoute>): PageRoute {
        val index = stack.lastIndex
        val record = stack.removeAt(index)
        if (record.index == RECORD_INDEX_START) {
            lastIndexes.remove(record.url)
        } else {
            lastIndexes[record.url] = record.index - RECORD_INDEX_STEP
        }
        record.removeNotify()
        Log.e("Thrio", "stack pop url ${record.url} index ${record.index}")
        return record
    }

    fun hasRoute(): Boolean {
        return subStack.isNotEmpty()
    }

    fun hasRoute(key: Long): Boolean {
        return subStack[key]?.isNotEmpty() ?: false
    }

    fun hasRoute(url: String): Boolean {
        return lastIndexes[url].let { it != null && it >= RECORD_INDEX_START }
    }

    fun hasRoute(url: String, index: Int): Boolean {
        return lastIndexes[url].let { it != null && it >= RECORD_INDEX_START && it >= index }
    }

    fun last(): PageRoute {
        val subStack = subStack
        for (it in subStack.size - 1 downTo 0 step 1) {
            val stack = subStack.valueAt(it)
            if (stack.isEmpty()) {
                continue
            }
            return stack.last()
        }
        throw IllegalArgumentException("stack is empty")
    }

    fun last(key: Long): PageRoute {
        val subStack = subStack
        val stack = subStack[key] ?: throw IllegalArgumentException("this key not in stack")
        require(stack.isNotEmpty()) { "stack is empty with key $key" }
        return stack.last()
    }

    fun last(url: String, index: Int): PageRoute {
        val subStack = subStack
        for (it in subStack.size - 1 downTo 0 step 1) {
            val stack = subStack.valueAt(it)
            if (stack.isEmpty()) {
                continue
            }
            return stack.lastOrNull { it.url == url && it.index <= index } ?: continue
        }
        throw IllegalArgumentException("stack is empty")
    }

    fun first(key: Long): PageRoute {
        val subStack = subStack
        val stack = subStack[key] ?: throw IllegalArgumentException("this key not in stack")
        require(stack.isNotEmpty()) { "stack is empty with key $key" }
        return stack.first()
    }

    fun all(key: Long): List<PageRoute> {
        val subStack = subStack
        val stack = subStack[key] ?: throw IllegalArgumentException("this key not in stack")
        require(stack.isNotEmpty()) { "stack is empty with key $key" }
        return stack
    }

    fun lastIndex(url: String): Int {
        return lastIndexes[url] ?: throw IllegalArgumentException("url $url index not found")
    }
}