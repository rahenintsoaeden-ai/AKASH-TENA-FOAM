package mg.akash.tenafoam;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    final String[] CATS={"AKASH MOYENNE","AKASH FORTE","TENA FOAM UNE PLACE","TENA FOAM DEUX PLACES"};
    final int[] THICK={8,10,12,14,16,18,20,22,24};
    DB db; LinearLayout root, body; TextView title;
    final Locale fr=new Locale("fr","FR");
    @Override public void onCreate(Bundle b){super.onCreate(b); db=new DB(this); home();}
    String money(long n){return String.format(fr,"%,d Ar",n).replace('\u202f',' ');}
    void setup(String heading){root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xfff4f6f8);
        title=new TextView(this);title.setText(heading);title.setTextSize(21);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xff17324d);title.setPadding(18,20,18,14);root.addView(title);
        ScrollView sc=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(14,4,14,20);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); Button back=button("⌂  ACCUEIL"); back.setOnClickListener(v->home()); root.addView(back);}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setPadding(8,10,8,10);return b;}
    void addButton(String s,Runnable r){Button b=button(s);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,5,0,5);body.addView(b,lp);b.setOnClickListener(v->r.run());}
    TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(16);t.setTextColor(0xff263746);t.setPadding(8,8,8,8);return t;}
    EditText input(String hint,String val, int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setText(val);e.setInputType(type);return e;}
    void home(){setup("AKASH & TENA FOAM\nGESTION DE STOCK");
        body.addView(text("Gestion hors connexion • Montants en Ariary (Ar)"));
        for(String c:CATS)addButton(c,()->category(c));
        addButton("＋ NOUVELLE VENTE",()->saleDialog());
        addButton("＋ ENTRÉE DE STOCK",()->stockDialog());
        addButton("STOCK RESTANT",()->stockPage());
        addButton("CHIFFRE D'AFFAIRES",()->revenuePage());
        addButton("JOURNAL DES VENTES",()->journalPage());
        addButton("EXPORTER LES VENTES (CSV)",()->exportCsv());
        addButton("SAUVEGARDE / RESTAURATION",()->backupInfo());
    }
    void category(String cat){setup(cat);
        Cursor c=db.getReadableDatabase().rawQuery("SELECT thick,qty,price FROM products WHERE cat=? ORDER BY thick",new String[]{cat});
        while(c.moveToNext()){final int th=c.getInt(0), qty=c.getInt(1);final long price=c.getLong(2);
            body.addView(text(th+" cm  •  Stock: "+qty+"  •  Prix unitaire: "+money(price)+"\nValeur du stock: "+money(price*qty)));
            addButton("Modifier stock/prix — "+th+" cm",()->editProduct(cat,th,qty,price));
        } c.close();
    }
    void editProduct(String cat,int th,int qty,long price){
        LinearLayout box=new LinearLayout(this);box.setPadding(10,0,10,0);box.setOrientation(1);
        EditText q=input("Quantité en stock",String.valueOf(qty),2), p=input("Prix unitaire (Ar)",String.valueOf(price),2);box.addView(q);box.addView(p);
        new AlertDialog.Builder(this).setTitle(cat+" — "+th+" cm").setView(box).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
            try{db.getWritableDatabase().execSQL("UPDATE products SET qty=?,price=? WHERE cat=? AND thick=?",new Object[]{Integer.parseInt(q.getText().toString()),Long.parseLong(p.getText().toString()),cat,th});category(cat);}
            catch(Exception e){msg("Valeurs invalides.");}}).show();
    }
    void productPicker(String title, ProductAction action){
        String[] options=new String[CATS.length*THICK.length];int k=0;for(String c:CATS)for(int t:THICK)options[k++]=c+" — "+t+" cm";
        new AlertDialog.Builder(this).setTitle(title).setItems(options,(d,which)->action.run(CATS[which/THICK.length],THICK[which%THICK.length])).show();
    }
    interface ProductAction{void run(String cat,int thick);}
    void saleDialog(){productPicker("Choisir le produit", (cat,th)->{
        Cursor cur=db.getReadableDatabase().rawQuery("SELECT qty,price FROM products WHERE cat=? AND thick=?",new String[]{cat,""+th});cur.moveToFirst();int avail=cur.getInt(0);long price=cur.getLong(1);cur.close();
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(12,0,12,0);
        box.addView(text("Disponible: "+avail+" • Prix actuel: "+money(price)));
        EditText q=input("Quantité vendue", "1",2),p=input("Prix de vente unitaire (Ar)",String.valueOf(price),2);box.addView(q);box.addView(p);
        new AlertDialog.Builder(this).setTitle("Nouvelle vente").setView(box).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
            try{int n=Integer.parseInt(q.getText().toString());long unit=Long.parseLong(p.getText().toString());if(n<=0||n>avail||unit<0){msg("Quantité ou prix invalide. Stock disponible: "+avail);return;}
                SQLiteDatabase x=db.getWritableDatabase();x.beginTransaction();try{x.execSQL("UPDATE products SET qty=qty-?, price=? WHERE cat=? AND thick=?",new Object[]{n,unit,cat,th});
                    x.execSQL("INSERT INTO sales(date,cat,thick,qty,price,total) VALUES(?,?,?,?,?,?)",new Object[]{System.currentTimeMillis(),cat,th,n,unit,unit*n});x.setTransactionSuccessful();}finally{x.endTransaction();}
                msg("Vente enregistrée : "+money(unit*n));home();
            }catch(Exception e){msg("Veuillez saisir des nombres valides.");}}).show();
    });}
    void stockDialog(){productPicker("Choisir le produit à approvisionner",(cat,th)->{
        EditText q=input("Quantité à ajouter","1",2);
        new AlertDialog.Builder(this).setTitle("Entrée de stock").setView(q).setNegativeButton("Annuler",null).setPositiveButton("Ajouter",(d,w)->{try{int n=Integer.parseInt(q.getText().toString());if(n<=0)throw new Exception();db.getWritableDatabase().execSQL("UPDATE products SET qty=qty+? WHERE cat=? AND thick=?",new Object[]{n,cat,th});msg("Stock mis à jour.");category(cat);}catch(Exception e){msg("Quantité invalide.");}}).show();
    });}
    void stockPage(){setup("STOCK RESTANT");int grand=0;
        for(String cat:CATS){int total=0;body.addView(text("━━ "+cat+" ━━"));Cursor c=db.getReadableDatabase().rawQuery("SELECT thick,qty FROM products WHERE cat=? ORDER BY thick",new String[]{cat});
            while(c.moveToNext()){int q=c.getInt(1);total+=q;body.addView(text(c.getInt(0)+" cm : "+q+" matelas"));}c.close();grand+=total;body.addView(text("Total catégorie : "+total));}
        body.addView(text("TOTAL GÉNÉRAL : "+grand+" matelas"));
    }
    void revenuePage(){setup("CHIFFRE D'AFFAIRES");
        addButton("Aujourd'hui",()->revenueFor(0));addButton("7 derniers jours",()->revenueFor(1));addButton("Mois en cours",()->revenueFor(2));
        addButton("Période personnalisée",()->customPeriod());
        Cursor c=db.getReadableDatabase().rawQuery("SELECT cat,thick,SUM(total) FROM sales GROUP BY cat,thick ORDER BY cat,thick",null);
        body.addView(text("Cumul par catégorie et épaisseur"));
        while(c.moveToNext())body.addView(text(c.getString(0)+" • "+c.getInt(1)+" cm : "+money(c.getLong(2))));c.close();
    }
    void revenueFor(int mode){long now=System.currentTimeMillis();Calendar cal=Calendar.getInstance();cal.setTimeInMillis(now);cal.set(Calendar.HOUR_OF_DAY,0);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);long start=cal.getTimeInMillis();
        if(mode==1)start-=7L*86400000;if(mode==2){cal.set(Calendar.DAY_OF_MONTH,1);start=cal.getTimeInMillis();}
        long sum=scalar("SELECT COALESCE(SUM(total),0) FROM sales WHERE date>=?",new String[]{""+start});
        new AlertDialog.Builder(this).setTitle("Chiffre d'affaires").setMessage("Total de la période : "+money(sum)).setPositiveButton("OK",null).show();
    }
    void customPeriod(){Calendar cal=Calendar.getInstance();DatePickerDialog start=new DatePickerDialog(this,(v,y,m,d)->{Calendar a=Calendar.getInstance();a.set(y,m,d,0,0,0);a.set(Calendar.MILLISECOND,0);long s=a.getTimeInMillis();
        DatePickerDialog end=new DatePickerDialog(this,(v2,y2,m2,d2)->{Calendar z=Calendar.getInstance();z.set(y2,m2,d2,23,59,59);long e=z.getTimeInMillis();long sum=scalar("SELECT COALESCE(SUM(total),0) FROM sales WHERE date BETWEEN ? AND ?",new String[]{""+s,""+e});
            new AlertDialog.Builder(this).setTitle("Période personnalisée").setMessage(money(sum)).setPositiveButton("OK",null).show();},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));end.setTitle("Date de fin");end.show();},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));start.setTitle("Date de début");start.show();}
    void journalPage(){setup("JOURNAL DES VENTES");Cursor c=db.getReadableDatabase().rawQuery("SELECT date,cat,thick,qty,price,total FROM sales ORDER BY date DESC",null);
        if(c.getCount()==0)body.addView(text("Aucune vente enregistrée."));
        SimpleDateFormat f=new SimpleDateFormat("dd/MM/yyyy HH:mm",fr);
        while(c.moveToNext())body.addView(text(f.format(new Date(c.getLong(0)))+"\n"+c.getString(1)+" — "+c.getInt(2)+" cm\nQuantité: "+c.getInt(3)+" × "+money(c.getLong(4))+"\nTOTAL : "+money(c.getLong(5))));
        c.close();
    }
    long scalar(String sql,String[] args){Cursor c=db.getReadableDatabase().rawQuery(sql,args);c.moveToFirst();long n=c.getLong(0);c.close();return n;}
    void exportCsv(){try{
        File f=new File(getExternalFilesDir(null),"ventes_akash_tenafoam.csv");PrintWriter p=new PrintWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
        p.println("Date;Categorie;Epaisseur_cm;Quantite;Prix_unitaire_Ar;Total_Ar");Cursor c=db.getReadableDatabase().rawQuery("SELECT date,cat,thick,qty,price,total FROM sales ORDER BY date",null);SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",fr);
        while(c.moveToNext())p.println(df.format(new Date(c.getLong(0)))+";"+c.getString(1)+";"+c.getInt(2)+";"+c.getInt(3)+";"+c.getLong(4)+";"+c.getLong(5));c.close();p.close();
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/csv");i.putExtra(Intent.EXTRA_SUBJECT,"Export ventes AKASH & TENA FOAM");i.putExtra(Intent.EXTRA_TEXT,"Fichier CSV enregistré ici : "+f.getAbsolutePath());startActivity(Intent.createChooser(i,"Partager l'emplacement du CSV"));
        msg("CSV créé dans le dossier de l'application : "+f.getAbsolutePath());
    }catch(Exception e){msg("Erreur lors de l'export CSV.");}}
    void backupInfo(){new AlertDialog.Builder(this).setTitle("Sauvegarde locale").setMessage("Les données sont enregistrées dans la base SQLite du téléphone et persistent après fermeture.\n\nPour éviter toute perte, exportez régulièrement le journal en CSV. La restauration automatique de la base complète n'est pas encore incluse dans cette version.").setPositiveButton("OK",null).show();}
    void msg(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"akash_tena_stock.db",null,1);}
        public void onCreate(SQLiteDatabase x){x.execSQL("CREATE TABLE products(cat TEXT NOT NULL, thick INTEGER NOT NULL, qty INTEGER NOT NULL DEFAULT 0, price INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(cat,thick))");
            x.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,date INTEGER,cat TEXT,thick INTEGER,qty INTEGER,price INTEGER,total INTEGER)");
            for(String c:CATS)for(int t:THICK)x.execSQL("INSERT INTO products(cat,thick,qty,price) VALUES(?,?,0,0)",new Object[]{c,t});}
        public void onUpgrade(SQLiteDatabase x,int oldV,int newV){}
    }
}
