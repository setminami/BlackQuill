package org.blackquill.engine

import scala.collection.immutable.Traversable
import scala.collection.mutable.ArrayBuffer

class TreeNode[T](val content:T) extends Traversable[T] {
 override def toString="<Node %s>".format(content.toString)

    var parent :Option[TreeNode[T]]=None
    val children=ArrayBuffer[TreeNode[T]]()

    def add(child :TreeNode[T])={
        child.parent=Some(this)
        children.append(child)
        child
    }

    def foreach[U](f: T => U){
        for(child <- children){
            f(child.content)
            child.foreach(f)
        }
    }
}