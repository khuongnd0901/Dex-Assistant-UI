package com.khuongnd.dexassistant.ui.screen

import org.junit.Assert.*
import org.junit.Test

class SemanticTargetIdentityTest {
    private val bounds = ScreenRect(0,0,1280,720)
    private val button = UiNodeRecord("root/0","root",ScreenRect(10,10,200,80),"Tiếp tục","","button",true,false,emptySet(),"node-1")
    private val window = ScreenWindow(10,1,1,bounds,true,true)
    private val tree = DesktopUiAccess.Tree(2,bounds,"fixture",10,listOf(window),listOf(button))
    private val target = DesktopUiAccess.Target(tree,AppUiTarget(button,"Tiếp tục"))
    @Test fun displayTopologyNodeIdentityAndLabelsMustStillMatch() {
        assertTrue(target.unchanged(tree.copy()))
        listOf(tree.copy(displayId=0),tree.copy(bounds=ScreenRect(0,0,720,1280)),tree.copy(packageName="another"),
            tree.copy(windowId=11),tree.copy(windows=listOf(window.copy(focused=false))),
            tree.copy(nodes=emptyList()),tree.copy(nodes=listOf(button.copy(text="Gửi"))),
            tree.copy(nodes=listOf(button.copy(identity="replacement-node"))),
            tree.copy(nodes=listOf(button.copy(bounds=ScreenRect(20,10,210,80)))))
            .forEach { assertFalse(target.unchanged(it)) }
        assertFalse(target.unchanged(null))
    }
    @Test fun descendantTextAndStructureRemainBoundToSelectedCard() {
        val title = button.copy(path="root/0/title",parentPath="root/0",clickable=false,identity="title-1")
        val cardTree = tree.copy(nodes=listOf(button,title))
        val card = DesktopUiAccess.Target(cardTree,AppUiTarget(button,"Tiếp tục"))
        assertTrue(card.unchanged(cardTree.copy()))
        assertFalse(card.unchanged(cardTree.copy(nodes=listOf(button,title.copy(text="Khác")))))
        assertFalse(card.unchanged(cardTree.copy(nodes=listOf(button))))
    }
}
