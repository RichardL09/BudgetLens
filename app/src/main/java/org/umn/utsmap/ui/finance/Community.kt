package org.umn.utsmap.ui.finance

data class FinanceGroup(val id: String, val name: String, val members: List<BillMember>, val creatorId: String, val code: String)
data class FinanceNotice(val id: String, val title: String, val description: String, val type: String, val target: String? = null, val read: Boolean = false)
