package org.umn.utsmap.data

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object FinancePdf {
    fun write(output: OutputStream, owner: String, period: String, entries: List<Transaction>) {
        val document=PdfDocument()
        val ink=Paint(Paint.ANTI_ALIAS_FLAG)
        var page:PdfDocument.Page?=null;var y=0f;var number=0
        val green=Color.rgb(22,130,64); val grey=Color.rgb(224,232,227)
        fun finish(){page?.let{p->ink.color=Color.GRAY;ink.textSize=9f;ink.typeface=Typeface.DEFAULT;p.canvas.drawText("BudgetLens | Halaman $number",40f,810f,ink);document.finishPage(p)};page=null}
        fun start(){finish();number++;page=document.startPage(PdfDocument.PageInfo.Builder(595,842,number).create());ink.color=green;ink.textSize=20f;ink.typeface=Typeface.DEFAULT_BOLD;page!!.canvas.drawText("BudgetLens",40f,44f,ink);ink.textSize=10f;ink.typeface=Typeface.DEFAULT;ink.color=Color.DKGRAY;page!!.canvas.drawText("Laporan Keuangan - $period",40f,64f,ink);y=88f}
        fun wrap(text:String,width:Float,size:Float=9f):List<String>{
            ink.textSize=size;ink.typeface=Typeface.DEFAULT
            val lines=mutableListOf<String>()
            text.split('\n').forEach{part->var rest=part.ifEmpty{" "};while(rest.isNotEmpty()){val count=ink.breakText(rest,true,width,null).coerceAtLeast(1);lines.add(rest.take(count));rest=rest.drop(count)}}
            return lines
        }
        fun text(value:String,bold:Boolean=false){val lines=wrap(value,515f,10f);lines.forEach{line->if(y>777)start();ink.textSize=10f;ink.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;ink.color=Color.rgb(20,35,29);page!!.canvas.drawText(line,40f,y,ink);y+=16f}}
        fun table(headers:List<String>,widths:List<Float>,rows:List<List<String>>,rightColumn:Int=-1){
            fun header(){if(y>742)start();var x=40f;headers.forEachIndexed{i,label->ink.style=Paint.Style.FILL;ink.color=green;page!!.canvas.drawRect(x,y,x+widths[i],y+25,ink);ink.color=Color.WHITE;ink.textSize=9f;ink.typeface=Typeface.DEFAULT_BOLD;page!!.canvas.drawText(label,x+6,y+16,ink);x+=widths[i]};y+=25}
            header()
            rows.forEachIndexed{rowIndex,row->
                val lines=row.mapIndexed{i,value->wrap(value,widths[i]-12f)}
                val height=(lines.maxOf{it.size}*13+12).toFloat()
                if(y+height>785){start();header()}
                var x=40f
                lines.forEachIndexed{i,cell->
                    ink.style=Paint.Style.FILL;ink.color=if(rowIndex%2==0)Color.rgb(244,249,246) else Color.WHITE
                    page!!.canvas.drawRect(x,y,x+widths[i],y+height,ink)
                    ink.style=Paint.Style.STROKE;ink.strokeWidth=.5f;ink.color=grey;page!!.canvas.drawRect(x,y,x+widths[i],y+height,ink)
                    ink.style=Paint.Style.FILL;ink.color=Color.rgb(20,35,29);ink.textSize=9f;ink.typeface=Typeface.DEFAULT
                    cell.forEachIndexed{index,line->val left=if(i==rightColumn)x+widths[i]-6-ink.measureText(line) else x+6;page!!.canvas.drawText(line,left,y+15+index*13,ink)}
                    x+=widths[i]
                };y+=height
            };y+=16
        }
        try{
            start();text("Pemilik: $owner");text("Dibuat: ${java.time.LocalDate.now()} | ${entries.size} transaksi")
            val income=entries.filter{it.income}.sumOf{it.amount};val expense=entries.filterNot{it.income}.sumOf{it.amount}
            table(listOf("Pemasukan","Pengeluaran","Saldo Bersih"),listOf(171f,172f,172f),listOf(listOf(rupiah(income),rupiah(expense),rupiah(income-expense))))
            text("RINCIAN TRANSAKSI",true)
            table(listOf("No","Tanggal","Transaksi / Kategori","Jenis","Nominal"),listOf(26f,68f,235f,60f,126f),entries.mapIndexed{i,tx->listOf("${i+1}",tx.date,"${tx.title}\n${tx.category}"+(if(tx.note.isBlank())"" else "\n${tx.note}"),if(tx.income)"Masuk" else "Keluar",rupiah(tx.amount))},4)
            if(entries.isEmpty())text("Tidak ada transaksi pada periode ini.")
            entries.filter{it.receipt!=null}.forEach{tx->
                if(y>665)start()
                val receipt=tx.receipt!!
                text("ITEM STRUK: ${tx.title} (${tx.date})",true)
                table(listOf("Item","Jumlah","Harga Satuan","Total"),listOf(235f,50f,115f,115f),receipt.items.map{listOf(it.name,it.quantity.toString(),rupiah(it.unitPrice),rupiah(it.total))},3)
                text("Subtotal ${rupiah(receipt.subtotal)} | PPN ${rupiah(receipt.tax)} | Total struk ${rupiah(receipt.total)}")
                if(tx.split)text("Pengeluaran pribadi yang dicatat: ${rupiah(tx.amount)}")
                y+=8
            }
            finish();document.writeTo(output)
        }finally{document.close()}
    }
}
