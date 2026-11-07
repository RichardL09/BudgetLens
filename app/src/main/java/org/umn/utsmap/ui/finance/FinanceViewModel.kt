package org.umn.utsmap.ui.finance

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import org.umn.utsmap.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

object TransactionValidation {
    fun amount(value: String): String? = when {
        value.isEmpty() -> "Nominal wajib diisi."
        value.any { it !in '0'..'9' } -> "Nominal hanya boleh berisi angka."
        value.toLongOrNull() == null -> "Nominal terlalu besar."
        value.toLong() <= 0 -> "Nominal harus lebih dari Rp 0."
        value.toLong() > 999999999999L -> "Nominal maksimal Rp 999.999.999.999."
        else -> null
    }
    fun title(value: String): String? = when {
        value.isBlank() -> "Nama transaksi wajib diisi."
        value.trim().length > 80 -> "Nama transaksi maksimal 80 karakter."
        else -> null
    }
    fun date(value: String): String? = try {
        val parsed = LocalDate.parse(value)
        if (parsed.year !in 2000..2100) "Pilih tahun 2000–2100." else null
    } catch (_: Exception) { "Tanggal tidak valid. Gunakan YYYY-MM-DD." }
}

class FinanceViewModel : ViewModel() {
    private val accounts = mutableMapOf<String, SnapshotStateList<Transaction>>()
    private var entries by mutableStateOf(DemoFinance.transactions.toMutableStateList())
    var month by mutableStateOf(YearMonth.of(2026, 10))
        private set
    val monthLabel: String get() = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID")))
    var transactionDate by mutableStateOf<LocalDate?>(null)
        private set
    var budgetMonth by mutableStateOf(YearMonth.now())
        private set
    var budgetDate by mutableStateOf<LocalDate?>(null)
        private set
    val homeMonth: YearMonth get() = YearMonth.now()
    fun label(period: YearMonth) = period.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID")))
    private fun rawTransactions(period: YearMonth, day: LocalDate? = null) = entries.filter {
        YearMonth.from(LocalDate.parse(it.date)) == period && (day == null || it.date == day.toString())
    }.sortedByDescending { it.date }
    private fun displayTransactions(period: YearMonth, day: LocalDate? = null) = rawTransactions(period, day).map { tx ->
        if(tx.income) tx else tx.copy(category = budgetsFor(period).find { it.sourceCategory == tx.category }?.category ?: tx.category)
    }
    val transactions get() = displayTransactions(month, transactionDate)
    private var accountKey by mutableStateOf("demo")
    private val budgetOverrides = mutableStateMapOf<String, List<Budget>>()
    fun budgetsFor(period: YearMonth) = budgetOverrides["$accountKey/$period"] ?: DemoFinance.budgets
    val budgets get() = budgetsFor(budgetMonth)
    val transactionBudgets get() = budgetsFor(month)
    val homeBudgets get() = budgetsFor(homeMonth)
    val homeTransactions get() = displayTransactions(homeMonth)
    val homeIncome get() = homeTransactions.filter { it.income }.sumOf { it.amount }
    val homeExpenses get() = homeTransactions.filterNot { it.income }.sumOf { it.amount }
    val homeBalance get() = homeIncome - homeExpenses
    val budgetExpenses get() = rawTransactions(budgetMonth, budgetDate).filterNot { it.income }.sumOf { it.amount }
    fun homeSpent(budget: Budget) = rawTransactions(homeMonth).filter { !it.income && it.category == budget.sourceCategory }.sumOf { it.amount }
    fun budgetTransactions(budget: Budget) = rawTransactions(budgetMonth, budgetDate)
        .filter { !it.income && it.category == budget.sourceCategory }.map { it.copy(category = budget.category) }
    fun budgetSpent(budget: Budget) = budgetTransactions(budget).sumOf { it.amount }
    fun selectBudgetMonth(value: YearMonth) { if (value.year in 2000..2100) { budgetMonth = value; budgetDate = null } }
    fun selectTransactionMonth(value: YearMonth) { if (value.year in 2000..2100) { month = value; transactionDate = null } }
    fun selectBudgetDate(value: LocalDate?) { if (value != null) budgetMonth = YearMonth.from(value); budgetDate = value }
    fun selectTransactionDate(value: LocalDate?) { if (value != null) month = YearMonth.from(value); transactionDate = value }
    fun budgetNameError(name: String, editing: String? = null): String? = when {
        name.isBlank() -> "Nama budget wajib diisi."
        name.trim().length > 40 -> "Nama budget maksimal 40 karakter."
        budgets.any { it.sourceCategory != editing && (it.category.equals(name.trim(), true) || it.sourceCategory.equals(name.trim(), true)) } -> "Nama budget sudah digunakan."
        else -> null
    }
    fun upsertBudget(name: String, amount: String, editing: String? = null): Boolean {
        if (budgetNameError(name, editing) != null || TransactionValidation.amount(amount) != null) return false
        if (editing != null && budgets.none { it.sourceCategory == editing }) return false
        val updated = Budget(name.trim(), amount.toLong(), editing ?: name.trim())
        budgetOverrides["$accountKey/$budgetMonth"] = if (editing == null) budgets + updated
            else budgets.map { if (it.sourceCategory == editing) updated else it }
        return true
    }
    private val targets = mutableStateMapOf<String, Long>()
    private val alerts = mutableStateMapOf<String, Boolean>()
    private val reminders = mutableStateMapOf<String, Boolean>()
    var savingsTarget: Long
        get() = targets[accountKey] ?: 500000L
        set(value) { targets[accountKey] = value }
    var budgetAlerts: Boolean
        get() = alerts[accountKey] ?: true
        set(value) { alerts[accountKey] = value }
    var paymentReminders: Boolean
        get() = reminders[accountKey] ?: true
        set(value) { reminders[accountKey] = value }
    fun saveBudget(category: String, amount: String): Boolean {
        if (budgets.none { it.category == category } || TransactionValidation.amount(amount) != null) return false
        budgetOverrides["$accountKey/$budgetMonth"] = budgets.map { if (it.category == category) it.copy(limit = amount.toLong()) else it }
        return true
    }
    private val billsByAccount = mutableMapOf<String, SnapshotStateList<SplitBill>>()
    private var bills by mutableStateOf(mutableStateListOf<SplitBill>())
    val splitBills: List<SplitBill> get() = bills.filter { it.groupId == null }
    fun bill(id: String) = bills.find { it.id == id }
    fun addBill(bill: SplitBill) { bills.add(0, bill) }
    fun requestPayment(billId: String, personId: String, actorId: String): Boolean {
        val index = bills.indexOfFirst { it.id == billId }; if (index < 0) return false
        val bill = bills[index]
        val member = bill.members.find { it.id == personId } ?: return false
        if (actorId != personId || personId == bill.payerId || member.status != PaymentStatus.UNPAID || bill.share(personId) == 0L) return false
        bills[index] = bill.copy(members = bill.members.map { if (it.id == personId) it.copy(status = PaymentStatus.PENDING, paid = false) else it })
        return true
    }
    fun confirmPayment(billId: String, personId: String, actorId: String): Boolean {
        val index = bills.indexOfFirst { it.id == billId }; if (index < 0) return false
        val bill = bills[index]
        if (actorId != bill.payerId || bill.members.none { it.id == personId && it.status == PaymentStatus.PENDING }) return false
        bills[index] = bill.copy(members = bill.members.map { if (it.id == personId) it.copy(status = PaymentStatus.PAID, paid = true) else it })
        return true
    }
    var weeklySummary by mutableStateOf(true)
    var splitUpdates by mutableStateOf(true)
    var quietHours by mutableStateOf(false)
    fun periodEntries() = transactions.toList()
    private fun sampleBills(): SnapshotStateList<SplitBill> {
        val receipt = ReceiptDetails(listOf(ReceiptLine("Tisu", 3, 10000), ReceiptLine("Sabun", 4, 15000)), 90000, 9000)
        val members = listOf(BillMember("self", "Kamu", self = true), BillMember("dinda", "Dinda", paid = true), BillMember("fajar", "Fajar"))
        val quantities = listOf(mapOf("self" to 1, "dinda" to 2, "fajar" to 0), mapOf("self" to 2, "dinda" to 1, "fajar" to 1))
        val example = BillCalculator.build("Belanja Kos Bersama", "2026-10-20", receipt, members, SplitMethod.ITEMS, emptyList(), emptyMap(), quantities)!!
        return mutableStateListOf(example.copy(id = "example-dinda", creatorId = "dinda", payerId = "dinda", example = true))
    }
    fun delete(id: String): Boolean = entries.removeAll { it.id == id }
    val groups get() = groupDetails.map { group -> ExpenseGroup(group.id, group.name, group.members.size, groupReceivable(group.id) - groupDebt(group.id)) }
    val incomeCategories = listOf("Gaji", "Hadiah", "Uang Saku", "Bonus", "Usaha", "Investasi", "Lainnya")
    fun expenseCategories(date: String) = budgetsFor(YearMonth.from(LocalDate.parse(date)))

    val income get() = transactions.filter { it.income }.sumOf { it.amount }
    val expenses get() = transactions.filterNot { it.income }.sumOf { it.amount }
    val balance get() = income - expenses
    val debt get() = bills.filter { it.payerId != it.members.first { member -> member.self }.id }.sumOf { bill -> bill.members.filter { it.self && it.status != PaymentStatus.PAID }.sumOf { bill.share(it.id) } }
    val receivable get() = bills.filter { bill -> bill.payerId == bill.members.first { it.self }.id }.sumOf { bill -> bill.members.filter { !it.self && it.status != PaymentStatus.PAID }.sumOf { bill.share(it.id) } }
    fun spent(category: String): Long {
        val source = transactionBudgets.find { it.category == category || it.sourceCategory == category }?.sourceCategory ?: category
        return rawTransactions(month, transactionDate).filter { !it.income && it.category == source }.sumOf { it.amount }
    }
    fun activate(email: String) {
        accountKey = email
        bills = billsByAccount.getOrPut(email) { sampleBills() }
        entries = accounts.getOrPut(email) {
            (DemoFinance.transactions + bills.mapNotNull { bill ->
                val own = bill.members.firstOrNull { it.self } ?: return@mapNotNull null
                val amount = bill.share(own.id)
                if (amount == 0L) null else Transaction("seed-${bill.id}",bill.title,bill.category,amount,bill.date,split=true,receipt=bill.receipt,splitBillId=bill.id)
            }).toMutableStateList()
        }
        activeGroups = groupAccounts.getOrPut(email) { seedGroups() }
        if (bills.none { it.groupId != null }) seedGroupBills()

        month = YearMonth.of(2026, 10)
        transactionDate = null
        budgetMonth = YearMonth.now()
        budgetDate = null
    }
    private val groupAccounts = mutableMapOf<String, SnapshotStateList<FinanceGroup>>()
    private var activeGroups by mutableStateOf(seedGroups())
    val groupDetails get() = activeGroups.toList()
    fun group(id: String) = activeGroups.find { it.id == id }
    fun groupBills(id: String) = bills.filter { it.groupId == id }.sortedByDescending { it.date }
    fun groupDebt(id: String) = groupBills(id).filter { it.payerId != "self" }.sumOf { bill -> if (bill.members.first { it.self }.status != PaymentStatus.PAID) bill.share("self") else 0 }
    fun groupReceivable(id: String) = groupBills(id).filter { it.payerId == "self" }.sumOf { bill -> bill.members.filter { !it.self && it.status != PaymentStatus.PAID }.sumOf { bill.share(it.id) } }
    private fun seedGroups() = mutableStateListOf(
        FinanceGroup("g1","Kos Griya Asri", listOf(BillMember("self","Kamu",self=true),BillMember("marcelline","Marcelline","6281230000011"),BillMember("richard","Richard","6281230000012"),BillMember("dwiky","Dwiky","6281230000013")), "richard", "KOS2026"),
        FinanceGroup("g2","Trip Pantai Gunungkidul",listOf(BillMember("self","Kamu",self=true),BillMember("michael","Michael","6281230000014"),BillMember("richard","Richard","6281230000012")),"self","TRIP26"),
        FinanceGroup("g3","Kelompok Praktikum IF",listOf(BillMember("self","Kamu",self=true),BillMember("dwiky","Dwiky","6281230000013"),BillMember("michael","Michael","6281230000014")),"dwiky","LAB2026")
    )
    private fun seedGroupBills() {
        groupDetails.forEachIndexed { index, group ->
            val receipt = ReceiptDetails(listOf(ReceiptLine(listOf("Belanja kebutuhan kos","Sewa Mobil","Cetak laporan")[index],1,listOf(300000L,375000L,75000L)[index])),listOf(300000L,375000L,75000L)[index],0)
            val payer = if(index==1) "self" else group.creatorId
            val members = group.members.map { it.copy(paid=it.id==payer,status=if(it.id==payer) PaymentStatus.PAID else PaymentStatus.UNPAID) }
            val bill = BillCalculator.build(receipt.items[0].name,"2026-10-18",receipt,members,SplitMethod.EQUAL,emptyList(),emptyMap())!!.copy(groupId=group.id,creatorId=group.creatorId,payerId=payer,example=true,category=if(index==2) "Pendidikan" else "Lainnya")
            val tx=save(bill.title,bill.share("self").toString(),bill.category,bill.date,false,"",split=true,receipt=receipt,splitBillId=bill.id)
            addBill(bill.copy(transactionId=tx))
        }
    }
    fun createGroup(name: String, selfName: String = "Kamu", selfPhone: String = ""): String? {
        if (name.trim().length !in 2..60) return null
        val id=UUID.randomUUID().toString()
        activeGroups.add(FinanceGroup(id,name.trim(),listOf(BillMember("self",selfName,selfPhone,self=true)),"self",UUID.randomUUID().toString().take(8).uppercase()))
        return id
    }
    fun addGroupMember(groupId: String, member: BillMember): Boolean {
        val index=activeGroups.indexOfFirst { it.id==groupId }; if(index<0) return false
        val group=activeGroups[index]
        if(group.members.size>=20 || member.name.trim().length !in 2..80 || group.members.any { it.id==member.id || it.name.equals(member.name,true) || (member.phone.isNotBlank() && it.phone==member.phone) }) return false
        activeGroups[index]=group.copy(members=group.members + member)
        return true
    }
    fun joinGroup(code: String, selfName: String = "Kamu", selfPhone: String = ""): String? {
        val normalized=code.trim().uppercase()
        activeGroups.find { it.code==normalized }?.let { return it.id }
        val source=groupAccounts.entries.firstNotNullOfOrNull { (owner,groups) -> groups.find{it.code==normalized}?.let{owner to it} }
        if(source!=null) {
            val (owner, original)=source
            val mapped=original.members.map{it.copy(id=if(it.self)owner else it.id,self=false)}
            val joined=original.copy(members=mapped + BillMember("self",selfName,selfPhone,self=true),creatorId=if(original.creatorId=="self")owner else original.creatorId)
            activeGroups.add(joined)
            val ownerGroups=groupAccounts[owner]!!;val index=ownerGroups.indexOfFirst{it.id==original.id}
            ownerGroups[index]=original.copy(members=original.members + BillMember(accountKey,selfName,selfPhone))
            return joined.id
        }
        if(normalized!="JOIN2026") return null
        val group=FinanceGroup("joined", "Komunitas BudgetLens",listOf(BillMember("michael","Michael","6281230000014"),BillMember("self","Kamu",self=true)),"michael",normalized)
        activeGroups.add(group); return group.id
    }
    fun saveGroupExpense(groupId: String, receipt: ReceiptDetails, title: String, date: String, category: String, payer: String, creator: String, counts: List<Map<String,Int>>, method: SplitMethod): String? {
        val group=group(groupId) ?: return null
        if(group.members.none { it.id==creator } || group.members.none { it.id==payer }) return null
        val members=group.members.map { it.copy(paid=it.id==payer,status=if(it.id==payer) PaymentStatus.PAID else PaymentStatus.UNPAID) }
        val built=BillCalculator.build(title,date,receipt,members,method,emptyList(),emptyMap(),counts)?.copy(groupId=groupId,creatorId=creator,payerId=payer,category=category) ?: return null
        val own=built.share("self")
        val tx=if(own>0) save(title,own.toString(),category,date,false,"Pengeluaran grup: ${group.name}",true,receipt,built.id) ?: return null else null
        addBill(built.copy(transactionId=tx)); return built.id
    }
    fun updateGroupAllocation(id: String, counts: List<Map<String,Int>>, category: String): Boolean {
        val index=bills.indexOfFirst { it.id==id }; if(index<0 || bills[index].groupId==null) return false
        val old=bills[index]
        if(old.members.any { it.id!=old.payerId && it.status!=PaymentStatus.UNPAID }) return false
        val currentMembers=group(old.groupId!!)?.members?.map { member -> old.members.find { it.id==member.id } ?: member } ?: return false
        val updated=BillCalculator.build(old.title,old.date,old.receipt,currentMembers,SplitMethod.ITEMS,emptyList(),emptyMap(),counts)?.copy(id=old.id,groupId=old.groupId,creatorId=old.creatorId,payerId=old.payerId,category=category,transactionId=old.transactionId) ?: return false
        val source=expenseCategories(old.date).find { it.category==category || it.sourceCategory==category }?.sourceCategory ?: return false
        val existing=entries.indexOfFirst { it.splitBillId==id }
        if(existing>=0) {
            if(updated.share("self")==0L) entries.removeAt(existing) else entries[existing]=entries[existing].copy(amount=updated.share("self"),category=source,sourceCategory=source)
        } else if(updated.share("self")>0) save(old.title,updated.share("self").toString(),category,old.date,false,"Pengeluaran grup",true,old.receipt,id)
        bills[index]=updated; return true
    }
    private val readNotifications = mutableStateMapOf<String,Set<String>>()
    fun notifications(): List<FinanceNotice> {
        val notices=mutableListOf<FinanceNotice>()
        if(budgetAlerts) homeBudgets.forEach { budget ->
            val spent=homeSpent(budget); val percent=spent*100/budget.limit
            if(percent>=80) notices.add(FinanceNotice("budget-${homeMonth}-${budget.sourceCategory}",if(percent>=100) "Budget ${budget.category} melewati batas!" else "Budget ${budget.category} hampir habis!","Terpakai ${rupiah(spent)} dari ${rupiah(budget.limit)} ($percent%).", "budget"))
        }
        if(paymentReminders || splitUpdates) bills.forEach { bill ->
            val self=bill.members.first { it.self }
            if(bill.payerId!=self.id && self.status!=PaymentStatus.PAID && paymentReminders) notices.add(FinanceNotice("bill-${bill.id}-${self.status}",if(self.status==PaymentStatus.PENDING) "Pembayaran menunggu konfirmasi" else "${bill.members.first { it.id==bill.payerId }.name} minta bayar", "${bill.title} • Bagianmu ${rupiah(bill.share(self.id))}","bill",bill.id))
            if(bill.payerId==self.id && splitUpdates) bill.members.filter { !it.self && it.status!=PaymentStatus.UNPAID }.forEach { member -> notices.add(FinanceNotice("paid-${bill.id}-${member.id}-${member.status}",if(member.status==PaymentStatus.PENDING) "${member.name} sudah menandai bayar" else "${member.name} sudah melunasi", "${bill.title} • ${rupiah(bill.share(member.id))}","bill",bill.id)) }
        }
        if(weeklySummary) notices.add(FinanceNotice("summary-$homeMonth-${homeTransactions.size}","Ringkasan keuangan tersedia","Pengeluaran ${label(homeMonth)}: ${rupiah(homeExpenses)}.","summary"))
        return notices.map { it.copy(read=it.id in (readNotifications[accountKey] ?: emptySet())) }
    }
    fun readNotice(id: String) { readNotifications[accountKey]=(readNotifications[accountKey] ?: emptySet()) + id }
    fun readAllNotices() { readNotifications[accountKey]=notifications().map { it.id }.toSet() }
    fun shiftMonth(offset: Long) { selectTransactionMonth(month.plusMonths(offset)) }
    fun find(id: String): Transaction? = entries.find { it.id == id }?.let { tx ->
        if(tx.income) tx else tx.copy(category = budgetsFor(YearMonth.from(LocalDate.parse(tx.date))).find { it.sourceCategory == tx.category }?.category ?: tx.category)
    }
    fun save(title: String, amount: String, category: String, date: String, income: Boolean, note: String, split: Boolean = false, receipt: ReceiptDetails? = null, splitBillId: String? = null): String? {
        if (TransactionValidation.title(title) != null || TransactionValidation.amount(amount) != null ||
            TransactionValidation.date(date) != null || note.length > 300 ||
            (!income && budgetsFor(YearMonth.from(LocalDate.parse(date))).none { it.category == category || it.sourceCategory == category })) return null
        val id = UUID.randomUUID().toString()
        val source = budgetsFor(YearMonth.from(LocalDate.parse(date))).find { it.category == category || it.sourceCategory == category }?.sourceCategory ?: category
        entries.add(0, Transaction(id, title.trim(), if (income) category else source,
            amount.toLong(), date, income, split = split, note = note.trim(), receipt = receipt, splitBillId = splitBillId))
        selectTransactionMonth(YearMonth.from(LocalDate.parse(date)))
        return id
    }
}
