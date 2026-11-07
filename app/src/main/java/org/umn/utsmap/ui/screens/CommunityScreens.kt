package org.umn.utsmap.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.umn.utsmap.ui.auth.*
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.data.*
import java.util.UUID

@Composable
internal fun CommunityPage(title: String, onBack: () -> Unit, systemPadding: Boolean = true, showBack: Boolean = true, titleBelowBack: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().then(if(systemPadding)Modifier.safeDrawingPadding() else Modifier).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            if (titleBelowBack) {
                if (showBack) AppBackButton(onBack)
                Text(title, style = MaterialTheme.typography.titleLarge)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showBack) AppBackButton(onBack)
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
            }
            content()
        }
    }
}
@Composable internal fun CommunityCard(modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier,shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant,contentColor=MaterialTheme.colorScheme.onSurface) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content) }
}
@Composable
internal fun FriendMemberPicker(auth: AuthViewModel, existing: List<BillMember>, onAdd: (BillMember)->Unit, onDismiss: ()->Unit) {
    var manual by rememberSaveable { mutableStateOf(false) }; var name by rememberSaveable { mutableStateOf("") }; var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Tambah Anggota")},text={
        Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { FilterChip(!manual,{manual=false},label={Text("Teman")}); FilterChip(manual,{manual=true},label={Text("Nama Manual")}) }
            if(manual) FigmaInput(name,{name=it;error=null},"Nama anggota",error=error) else {
                Text("Pilih dari daftar teman",style=MaterialTheme.typography.bodySmall)
                auth.friends.forEach { friend ->
                    val exists=existing.any { it.phone==friend.phone || it.name.equals(friend.name,true) }
                    Surface(onClick={ if(!exists) onAdd(BillMember(friend.id,friend.name,friend.phone)) },enabled=!exists,shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.surfaceVariant,contentColor=MaterialTheme.colorScheme.onSurface) {
                        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            AccountAvatar(friend.photo,friend.name,Modifier.size(40.dp)); Column(Modifier.weight(1f)) { Text(friend.name); Text("+${friend.phone}",style=MaterialTheme.typography.bodySmall); if(exists) Text("Sudah menjadi anggota",style=MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
                if(auth.friends.isEmpty()) Text("Belum ada teman. Tambahkan melalui Profil → Teman.")
            }
        }
    },confirmButton={ if(manual) TextButton(onClick={
        error=when { name.trim().length !in 2..80 -> "Nama harus 2–80 karakter."; existing.any { it.name.equals(name.trim(),true) } -> "Anggota sudah ditambahkan."; else -> null }
        if(error==null) onAdd(BillMember(UUID.randomUUID().toString(),name.trim()))
    }) {Text("Tambah")} },dismissButton={TextButton(onClick=onDismiss){Text("Tutup")}})
}
@Composable
fun FriendsScreen(auth: AuthViewModel,onBack:()->Unit) {
    var phone by rememberSaveable { mutableStateOf("") }; var message by remember { mutableStateOf<String?>(null) }
    CommunityPage("Teman",onBack,titleBelowBack=true) {
        FigmaInput(phone,{phone=it;message=null},"Cari nomor telepon",keyboard=KeyboardType.Phone)
        GradientButton(onClick={ message=auth.requestFriend(phone) ?: "Permintaan pertemanan dikirim." },modifier=Modifier.fillMaxWidth()){Text("Kirim Permintaan Pertemanan")}
        message?.let {Text(it)}
        if(auth.friendRequests.isNotEmpty()) Text("Permintaan Masuk",style=MaterialTheme.typography.titleMedium)
        auth.friendRequests.forEach { person -> CommunityCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){ AccountAvatar(person.photo,person.name,Modifier.size(40.dp)); Column {Text(person.name);Text("+${person.phone}",style=MaterialTheme.typography.bodySmall)} }
            Row {TextButton(onClick={auth.acceptFriend(person.id)}){Text("Terima")};TextButton(onClick={auth.rejectFriend(person.id)}){Text("Tolak")}}
        } }
        if(auth.sentRequests.isNotEmpty()) Text("Menunggu Persetujuan",style=MaterialTheme.typography.titleMedium)
        auth.sentRequests.forEach { person -> CommunityCard { Text(person.name);Text("+${person.phone}");TextButton(onClick={auth.cancelFriendRequest(person.id)}){Text("Batalkan Permintaan") } } }
        Text("Teman (${auth.friends.size})",style=MaterialTheme.typography.titleMedium)
        auth.friends.forEach { person -> CommunityCard { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){ AccountAvatar(person.photo,person.name,Modifier.size(44.dp));Column {Text(person.name);Text("+${person.phone}",style=MaterialTheme.typography.bodySmall)} } } }
    }
}
@Composable
fun GroupListScreen(finance: FinanceViewModel,onBack:()->Unit,onGroup:(String)->Unit,selfName:String="Kamu",selfPhone:String="") {
    var create by rememberSaveable { mutableStateOf(false) };var join by rememberSaveable {mutableStateOf(false)};var name by rememberSaveable{mutableStateOf("")};var error by rememberSaveable{mutableStateOf<String?>(null)}
    CommunityPage("Grup Saya",onBack,systemPadding=false,showBack=false) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Button(onClick={create=true;error=null},modifier=Modifier.weight(1f)){Text("+ Buat Grup")};OutlinedButton(onClick={join=true;error=null},modifier=Modifier.weight(1f)){Text("Gabung Kode")} }
        CommunityCard {
            Text("RINGKASAN SEMUA GRUP",style=MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)){Text("Total Hutangmu",style=MaterialTheme.typography.bodySmall);Text(rupiah(finance.groupDetails.sumOf{finance.groupDebt(it.id)}),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.titleMedium)}
                Column(Modifier.weight(1f)){Text("Total Piutangmu",style=MaterialTheme.typography.bodySmall);Text(rupiah(finance.groupDetails.sumOf{finance.groupReceivable(it.id)}),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleMedium)}
            }
        }
        finance.groups.forEach { group -> GroupSummaryCard(group){onGroup(group.id)} }
    }
    if(create || join) AlertDialog(onDismissRequest={create=false;join=false},title={Text(if(join)"Gabung Grup" else "Buat Grup")},text={Column {FigmaInput(name,{name=it;error=null},if(join)"Kode grup" else "Nama grup",error=error)}},confirmButton={TextButton(onClick={
        val id=if(join)finance.joinGroup(name,selfName,selfPhone) else finance.createGroup(name,selfName,selfPhone)
        if(id==null) error=if(join)"Kode tidak ditemukan." else "Nama grup harus 2–60 karakter." else {create=false;join=false;name="";onGroup(id)}
    }){Text(if(join)"Gabung" else "Buat")}},dismissButton={TextButton(onClick={create=false;join=false}){Text("Batal")}})
}
@Composable
fun GroupDetailScreen(id:String,finance:FinanceViewModel,auth:AuthViewModel,onBack:()->Unit,onAddExpense:()->Unit,onBill:(String)->Unit) {
    val group=finance.group(id) ?: return
    val context=LocalContext.current
    var add by remember{mutableStateOf(false)};var pay by remember{mutableStateOf(false)}
    fun share(text:String){ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"Bagikan")) }
    CommunityPage(group.name,onBack) {
        Text("${group.members.size} anggota • Pembuat: ${group.members.first { it.id==group.creatorId }.name}",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={add=true},modifier=Modifier.weight(1f)){Text("+ Anggota")};OutlinedButton(onClick={share("Gabung grup ${group.name} di BudgetLens. Kode: ${group.code}")},modifier=Modifier.weight(1f)){Text("Bagikan Kode")}}
        Text("Kode grup: ${group.code}",style=MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            CommunityCard(Modifier.weight(1f)){Text("KAMU BERHUTANG",style=MaterialTheme.typography.labelSmall);Text(rupiah(finance.groupDebt(id)),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.titleMedium)}
            CommunityCard(Modifier.weight(1f)){Text("KAMU DIUTANGI",style=MaterialTheme.typography.labelSmall);Text(rupiah(finance.groupReceivable(id)),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleMedium)}
        }
        Row(verticalAlignment=Alignment.CenterVertically){Text("Pengeluaran Grup",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);TextButton(onClick=onAddExpense){Text("+ Tambah")}}
        finance.groupBills(id).forEach { bill -> Surface(onClick={onBill(bill.id)},color=MaterialTheme.colorScheme.surfaceVariant,contentColor=MaterialTheme.colorScheme.onSurface,shape=MaterialTheme.shapes.medium){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row{Text(bill.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));Text(rupiah(bill.receipt.total),style=MaterialTheme.typography.titleSmall)};Text("Dibayar ${bill.members.first{it.id==bill.payerId}.name} • ${bill.date}",style=MaterialTheme.typography.bodySmall);Text("Ditambahkan ${bill.members.first { it.id==bill.creatorId }.name}",style=MaterialTheme.typography.bodySmall);Text("Bagianmu ${rupiah(bill.share("self"))}",color=MaterialTheme.colorScheme.primary)
        }} }
        if(finance.groupBills(id).isEmpty()) Text("Belum ada pengeluaran grup.")
        if(finance.groupDebt(id)>0) GradientButton(onClick={pay=true},modifier=Modifier.fillMaxWidth()){Text("Bayar Pengeluaran Grup")}
        if(finance.groupReceivable(id)>0) OutlinedButton(onClick={
            val text=finance.groupBills(id).filter{it.payerId=="self"}.flatMap{bill->bill.members.filter{!it.self && it.status!=PaymentStatus.PAID}.map{"${it.name}: ${rupiah(bill.share(it.id))} untuk ${bill.title}"}}.joinToString("\n")
            share("Pengingat pembayaran ${group.name}\n$text")
        },modifier=Modifier.fillMaxWidth()){Text("Ingatkan Anggota untuk Bayar")}
        Text("Anggota Grup",style=MaterialTheme.typography.titleMedium)
        group.members.forEach { member -> CommunityCard {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){AccountAvatar("",member.name,Modifier.size(40.dp));Column{Text(member.name+if(member.self && member.name!="Kamu")" (Kamu)" else "");Text(if(member.id==group.creatorId)"Pembuat Grup" else "Anggota",style=MaterialTheme.typography.bodySmall);if(member.phone.isNotBlank())Text("+${member.phone}",style=MaterialTheme.typography.bodySmall)}}} }
    }
    if(add) FriendMemberPicker(auth,group.members,{member->if(finance.addGroupMember(id,member))add=false},{add=false})
    if(pay) AlertDialog(onDismissRequest={pay=false},title={Text("Pilih Pengeluaran")},text={Column(Modifier.verticalScroll(rememberScrollState())){
        finance.groupBills(id).filter{it.payerId!="self" && it.members.first{m->m.self}.status!=PaymentStatus.PAID}.forEach{ bill->TextButton(onClick={pay=false;onBill(bill.id)}){Text("${bill.title} • ${rupiah(bill.share("self"))}")} }
    }},confirmButton={TextButton(onClick={pay=false}){Text("Tutup")}})
}
@Composable
fun GroupExpenseScreen(groupId:String,billId:String?,finance:FinanceViewModel,onBack:()->Unit,onSaved:(String)->Unit) {
    val group=finance.group(groupId) ?: return
    val existing=billId?.let(finance::bill)
    val draft=rememberSaveable(groupId, billId, saver = ReceiptDraftSaver) { ReceiptScanViewModel().apply{beginManual()} }
    val formStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    var step by rememberSaveable{mutableIntStateOf(if(existing==null)0 else 1)}
    var payer by rememberSaveable{mutableStateOf(existing?.payerId ?: "self")}
    val receipt=existing?.receipt ?: draft.receiptDetails()
    var method by rememberSaveable{mutableStateOf(if(existing==null)SplitMethod.EQUAL else SplitMethod.ITEMS)}
    var counts by rememberSaveable(stateSaver = AllocationSaver) {mutableStateOf(receipt.items.mapIndexed { index,item-> group.members.associate{member->member.id to (existing?.quantities?.getOrNull(index)?.get(member.id) ?: if(member.self)item.quantity else 0).toString()} })}
    var category by rememberSaveable{mutableStateOf(existing?.category ?: "Lainnya")}
    var error by rememberSaveable{mutableStateOf<String?>(null)}
    var submitted by rememberSaveable { mutableStateOf(false) }
    val back = { if (step == 1 && existing == null) step = 0 else onBack() }
    BackHandler(step==1 && existing==null, back)
    if(step==0) formStates.SaveableStateProvider("manual") {
        ManualBillScreen(draft,onBack,{ counts=draft.items.mapIndexed { index, item ->
            val previous = receipt.items.getOrNull(index)
            if (previous != null && previous.name == item.name && previous.quantity == item.quantity && previous.unitPrice == item.unitPrice)
                counts.getOrNull(index) ?: group.members.associate { it.id to if(it.self)item.quantity.toString() else "0" }
            else group.members.associate { it.id to if(it.self)item.quantity.toString() else "0" }
        }; submitted=false;error=null;step=1 },"Tambah Pengeluaran Grup")
    }
    else CommunityPage(if(existing==null)"Atur Pengeluaran Grup" else "Edit Pembagian Grup",back){
        Text(existing?.title ?: draft.merchant,style=MaterialTheme.typography.titleMedium)
        Text("Dibayar oleh",style=MaterialTheme.typography.labelMedium)
        if(existing==null) group.members.forEach{member->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(payer==member.id,{payer=member.id});Text(member.name)}} else Text(existing.members.first{it.id==payer}.name)
        Text("Kategori Budget")
        finance.expenseCategories(existing?.date ?: draft.date).forEach{budget->FilterChip(category==budget.category || category==budget.sourceCategory,{category=budget.category},label={Text(budget.category)})}
        if(existing==null) Row{FilterChip(method==SplitMethod.EQUAL,{method=SplitMethod.EQUAL;error=null},label={Text("Sama Rata")});FilterChip(method==SplitMethod.ITEMS,{method=SplitMethod.ITEMS;error=null},label={Text("Per Item")})}
        receipt.items.forEachIndexed{index,item->CommunityCard{
            Text("${item.name} ×${item.quantity} • ${rupiah(item.total)}")
            if(method==SplitMethod.ITEMS) group.members.forEach{member->FigmaInput(counts[index][member.id] ?: "0",{value->counts=counts.mapIndexed{i,row->if(i==index)row+(member.id to value.take(3)) else row};error=null},"Jumlah ${item.name} - ${member.name}",keyboard=KeyboardType.Number, error=if(submitted)numberError(counts[index][member.id] ?: "0",0,item.quantity.toLong(),"Jumlah") else null)}
            if (method == SplitMethod.ITEMS) {
                val assigned = counts[index].values.sumOf { it.toIntOrNull() ?: 0 }
                if (submitted && assigned != item.quantity) Text("Jumlah pembagian harus ${item.quantity}; saat ini $assigned.", color=MaterialTheme.colorScheme.error, style=MaterialTheme.typography.bodySmall)
            } else Text("Dibagi rata untuk ${group.members.size} anggota")
        }}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        GradientButton(onClick={
            submitted = true
            val parsed=counts.map{row->row.mapValues{if (it.value.any { digit -> digit !in '0'..'9' }) -1 else it.value.toIntOrNull() ?: -1}}
            val id=if(existing==null) finance.saveGroupExpense(groupId,receipt,draft.merchant,draft.date,category,payer,"self",parsed,method) else if(finance.updateGroupAllocation(existing.id,parsed,category))existing.id else null
            if(id==null) error="Periksa pembagian jumlah barang dan kategori. Pengeluaran yang sedang/sudah dibayar tidak dapat diubah." else onSaved(id)
        },modifier=Modifier.fillMaxWidth()){Text("Simpan Pengeluaran Grup")}
    }
}
@Composable
fun NotificationsScreen(finance:FinanceViewModel,onBack:()->Unit,onBill:(String)->Unit,onBudget:()->Unit,onSummary:()->Unit) {
    CommunityPage("Notifikasi",onBack){
        val notices=finance.notifications()
        Row(verticalAlignment=Alignment.CenterVertically){Text("${notices.count{!it.read}} belum dibaca",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick=finance::readAllNotices){Text("Tandai semua")}}
        listOf(false,true).forEach{read->
            val rows=notices.filter{it.read==read}
            if(rows.isNotEmpty())Text(if(read)"SEBELUMNYA" else "BARU",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            rows.forEach{notice->Surface(onClick={finance.readNotice(notice.id);when(notice.type){"bill"->notice.target?.let(onBill);"budget"->onBudget();else->onSummary()}},color=MaterialTheme.colorScheme.surfaceVariant,contentColor=MaterialTheme.colorScheme.onSurface,shape=MaterialTheme.shapes.medium){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row{Text(when(notice.type){"budget"->"⚠";"bill"->"↗";else->"▤"},modifier=Modifier.padding(end=12.dp));Text(notice.title,style=MaterialTheme.typography.titleSmall,modifier=Modifier.weight(1f));if(!notice.read)Text("●",color=MaterialTheme.colorScheme.primary)}
                Text(notice.description,style=MaterialTheme.typography.bodySmall)
                if(notice.type=="bill") Row(verticalAlignment=Alignment.CenterVertically) {
                    val bill=notice.target?.let(finance::bill)
                    Text(bill?.date.orEmpty(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f))
                    OutlinedButton(onClick={finance.readNotice(notice.id);notice.target?.let(onBill)},contentPadding=PaddingValues(horizontal=10.dp,vertical=0.dp),modifier=Modifier.height(30.dp)){Text(if(bill?.members?.firstOrNull{it.self}?.status==PaymentStatus.UNPAID && bill.payerId!="self") "Bayar Sekarang" else "Lihat Tagihan",style=MaterialTheme.typography.labelSmall)}
                }
            }}}
        }
        if(notices.isEmpty())Text("Belum ada notifikasi.")
    }
}
