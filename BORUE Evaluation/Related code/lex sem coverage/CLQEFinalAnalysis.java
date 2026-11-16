package org.borue  ;

import java.io.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//final class will calculate all metrics for paper 


/** CLQE final analysis. Compatible with Apache POI 3.17 and Java 8+. */
public class CLQEFinalAnalysis {
    static final String INPUT_PATH = "D:\\BORUE\\CLQE_Semantic_Evaluation_Final.xlsx";
    static final String OUTPUT_PATH = "D:\\BORUE\\CLQE_Java_Statistical_Analysis.xlsx";
    static final String[] V = {"var1","var2","var3","var4","var5","common"};
    static final String[] LABEL = {"var1","var2","var3","var4","var5","Common"};
    static final double Z = 1.959963984540054;
    static class Stats {
        long n,lex,sem,strict,recovered,miss,score0,score1,score2,score3;
        long b,c; // paired: lexical-only, semantic-only
        void add(boolean l, int score) {
            n++; if(l)lex++;else miss++;
            if(score>=2)sem++;if(score==3)strict++;
            if(!l && score>=2)recovered++;
            if(l && score<2)b++;if(!l && score>=2)c++;
            if(score==0)score0++;if(score==1)score1++;if(score==2)score2++;if(score==3)score3++;
        }
    }
    static String value(Row r,int col) {
        if(col<0)return "";
        Cell c=r.getCell(col); if(c==null)return "";
        if(c.getCellType()==Cell.CELL_TYPE_STRING)return c.getStringCellValue().trim();
        if(c.getCellType()==Cell.CELL_TYPE_NUMERIC)return Double.toString(c.getNumericCellValue());
        if(c.getCellType()==Cell.CELL_TYPE_BOOLEAN)return Boolean.toString(c.getBooleanCellValue());
        return "";
    }
    static int idx(Map<String,Integer> m,String key) {
        Integer i=m.get(key.toLowerCase(Locale.ROOT));
        if(i==null)throw new IllegalArgumentException("Missing column: "+key);
        return i;
    }
    static boolean yes(String s) {return s.equalsIgnoreCase("yes")||s.equalsIgnoreCase("true")||s.equals("1");}
    static int score(String s) {
        try {double d=Double.parseDouble(s);int k=(int)d;if(k<0||k>3||k!=d)throw new Exception();return k;}
        catch(Exception e){throw new IllegalArgumentException("Invalid score: "+s);}
    }
    static double pct(double a,double b){return b==0?0:100*a/b;}
    static double[] wilson(long x,long n){
        if(n==0)return new double[]{0,0};
        double p=(double)x/n,z2=Z*Z,den=1+z2/n;
        double mid=(p+z2/(2*n))/den;
        double half=Z*Math.sqrt(p*(1-p)/n+z2/(4.0*n*n))/den;
        return new double[]{100*(mid-half),100*(mid+half)};
    }
    static double erf(double x){ // Abramowitz-Stegun approximation
        double sign=x<0?-1:1;x=Math.abs(x);
        double t=1/(1+0.3275911*x);
        double y=1-(((((1.061405429*t-1.453152027)*t)+1.421413741)*t-0.284496736)*t+0.254829592)*t*Math.exp(-x*x);
        return sign*y;
    }
    static double mcnemarP(long b,long c){
        if(b+c==0)return 1;
        double chi=Math.pow(Math.max(0,Math.abs(b-c)-1),2)/(double)(b+c);
        return Math.max(0,1-erf(Math.sqrt(chi/2)));
    }
    static Row row(Sheet s,Object... cells){Row r=s.createRow(s.getPhysicalNumberOfRows());for(int i=0;i<cells.length;i++){
        Cell c=r.createCell(i);Object v=cells[i];if(v instanceof Number)c.setCellValue(((Number)v).doubleValue());else c.setCellValue(String.valueOf(v));}return r;}
    static void writeStats(Sheet s,String name,Stats x){
        double[] ci=wilson(x.sem,x.n),rec=wilson(x.recovered,x.miss);
        row(s,name,x.n,x.lex,pct(x.lex,x.n),x.sem,pct(x.sem,x.n),ci[0],ci[1],x.strict,pct(x.strict,x.n),
            x.miss,x.recovered,pct(x.recovered,x.miss),rec[0],rec[1],pct(x.sem-x.lex,x.n));
    }
    public static void main(String[] args)throws Exception{
        String in = INPUT_PATH;
        String out = OUTPUT_PATH;
        Workbook source;
        try(FileInputStream f=new FileInputStream(in)){source=new XSSFWorkbook(f);}
        Sheet data=source.getSheet("SortedTestBench");if(data==null)throw new IllegalArgumentException("Missing SortedTestBench sheet");
        Map<String,Integer> columns=new HashMap<String,Integer>();
        for(Cell c:data.getRow(0))if(c.getCellType()==Cell.CELL_TYPE_STRING)columns.put(c.getStringCellValue().trim().toLowerCase(Locale.ROOT),c.getColumnIndex());
        Stats[] by=new Stats[6];for(int i=0;i<6;i++)by[i]=new Stats();Stats total=new Stats();
        long[] ld=new long[7],sd=new long[7],strictD=new long[7];
        long[][] queryPaired=new long[2][2]; // any lexical vs any semantic
        long[] canonical=new long[2],variantRecovery=new long[2];
        long evaluated=0,errors=0;
        List<String[]> issues=new ArrayList<String[]>();
        for(int r=1;r<=data.getLastRowNum();r++){
            Row rr=data.getRow(r);if(rr==null)continue;
            String common=value(rr,idx(columns,"Common"));
            if(common.isEmpty())continue;
            evaluated++;int lCount=0,sCount=0,strictCount=0;boolean[] hits=new boolean[6];
            for(int j=0;j<6;j++){
                String v=V[j],hs=value(rr,idx(columns,"Hit_"+v));
                String ss=value(rr,idx(columns,"SemanticScore_"+v));
                boolean lex=yes(hs);int sc;
                try{sc=score(ss);}catch(Exception e){throw new IllegalArgumentException("Excel row "+(r+1)+", "+v+": "+e.getMessage());}
                boolean sem=sc>=2,strict=sc==3;
                by[j].add(lex,sc);total.add(lex,sc);hits[j]=sem;
                if(lex)lCount++;if(sem)sCount++;if(strict)strictCount++;
                String sh=value(rr,idx(columns,"SemanticHit_"+v));
                String st=value(rr,idx(columns,"StrictSemanticHit_"+v));
                if(yes(sh)!=sem){errors++;issues.add(new String[]{""+(r+1),v,"SemanticHit inconsistent",sh,""+sc});}
                if(yes(st)!=strict){errors++;issues.add(new String[]{""+(r+1),v,"StrictSemanticHit inconsistent",st,""+sc});}
                if(lex && sc!=3){errors++;issues.add(new String[]{""+(r+1),v,"Lexical Yes not score 3",hs,""+sc});}
            }
            ld[lCount]++;sd[sCount]++;strictD[strictCount]++;
            queryPaired[lCount>0?1:0][sCount>0?1:0]++;
            if(hits[5])canonical[1]++;else canonical[0]++;
            boolean anyVariant=false;for(int j=0;j<5;j++)if(hits[j])anyVariant=true;
            if(!hits[5] && anyVariant)variantRecovery[1]++;else variantRecovery[0]++;
            String countS=value(rr,idx(columns,"SemanticSuccessfulCount"));
            String countL=value(rr,idx(columns,"LexicalSuccessfulCount"));
            try{
                if(scoreCount(countS)!=sCount){errors++;issues.add(new String[]{""+(r+1),"query","SemanticSuccessfulCount inconsistent",countS,""+sCount});}
                if(scoreCount(countL)!=lCount){errors++;issues.add(new String[]{""+(r+1),"query","LexicalSuccessfulCount inconsistent",countL,""+lCount});}
            } catch (Exception e) {
                errors++;
                issues.add(new String[]{
                    "" + (r + 1),
                    "query",
                    "Invalid summary count",
                    countS + " / " + countL,
                    ""
                });
            }
        }
        Workbook output=new XSSFWorkbook();
        Sheet summary=output.createSheet("Overall");row(summary,"Measure","Value");
        row(summary,"Evaluated queries",evaluated);row(summary,"Query-variant pairs",total.n);
        row(summary,"Lexical hits",total.lex);row(summary,"Lexical hit rate (%)",pct(total.lex,total.n));
        row(summary,"Semantic hits (>=2)",total.sem);row(summary,"Semantic hit rate (%)",pct(total.sem,total.n));
        row(summary,"Strict hits (=3)",total.strict);row(summary,"Strict hit rate (%)",pct(total.strict,total.n));
        row(summary,"Lexical misses",total.miss);row(summary,"Recovered lexical misses",total.recovered);
        row(summary,"Recovery rate among misses (%)",pct(total.recovered,total.miss));
        row(summary,"Semantic minus lexical (percentage points)",pct(total.sem-total.lex,total.n));
        row(summary,"Queries with any lexical success",evaluated-ld[0]);
        row(summary,"Queries with any semantic success",evaluated-sd[0]);
        row(summary,"Canonical semantic success",canonical[1]);
        row(summary,"Noncanonical variant rescue of canonical failure",variantRecovery[1]);
        row(summary,"Integrity issues",errors);
        Sheet vs=output.createSheet("Variant_Results");
        row(vs,"Variant","N","Lexical Hits","Lexical %","Semantic Hits","Semantic %","Semantic 95% CI low","Semantic 95% CI high","Strict Hits","Strict %","Lexical Misses","Recovered Misses","Recovery %","Recovery CI low","Recovery CI high","Gain (pp)");
        for(int i=0;i<6;i++)writeStats(vs,LABEL[i],by[i]);writeStats(vs,"ALL",total);
        Sheet paired=output.createSheet("Paired_McNemar");
        row(paired,"Unit","N","Both Success","Lexical Only (b)","Semantic Only (c)","Both Fail","McNemar Chi2 (CC)","Approx p (two-sided)","Difference (pp)");
        for(int j=0;j<7;j++){
            Stats x=j==6?total:by[j];long both=x.lex-x.b;long neither=x.n-both-x.b-x.c;
            double chi=x.b+x.c==0?0:Math.pow(Math.max(0,Math.abs(x.b-x.c)-1),2)/(double)(x.b+x.c);
            row(paired,j==6?"ALL pairs (descriptive only)":LABEL[j],x.n,both,x.b,x.c,neither,chi,mcnemarP(x.b,x.c),pct(x.c-x.b,x.n));
        }
        long qb=queryPaired[1][0],qc=queryPaired[0][1];
        double qchi=qb+qc==0?0:Math.pow(Math.max(0,Math.abs(qb-qc)-1),2)/(double)(qb+qc);
        row(paired,"QUERY any success (primary)",evaluated,queryPaired[1][1],qb,qc,queryPaired[0][0],qchi,mcnemarP(qb,qc),pct(qc-qb,evaluated));
        Sheet dist=output.createSheet("Query_Distribution");row(dist,"Successful forms","Lexical Queries","Semantic Queries","Strict Queries","Lexical %","Semantic %","Strict %");
        for(int k=0;k<=6;k++)row(dist,k,ld[k],sd[k],strictD[k],pct(ld[k],evaluated),pct(sd[k],evaluated),pct(strictD[k],evaluated));
        Sheet grades=output.createSheet("Score_Distribution");row(grades,"Variant","Score 0","Score 1","Score 2","Score 3","Total");
        for(int j=0;j<7;j++){Stats x=j==6?total:by[j];row(grades,j==6?"ALL":LABEL[j],x.score0,x.score1,x.score2,x.score3,x.n);}
        Sheet audit=output.createSheet("Integrity_Issues");row(audit,"Excel row","Variant","Issue","Stored","Expected");
        for(String[] issue:issues)row(audit,(Object[])issue);
        Sheet notes=output.createSheet("Method_Notes");row(notes,"Item","Description");
        row(notes,"Data","SortedTestBench individual records; summary sheets not used");
        row(notes,"Unit","Six query-variant pairs per query; same queries across variants");
        row(notes,"Lexical","Original Java Hit_* flag; not re-normalized");
        row(notes,"Semantic","SemanticScore >= 2");row(notes,"Strict","SemanticScore = 3");
        row(notes,"Recovery","Lexical No and SemanticScore >= 2");
        row(notes,"95% intervals","Wilson score intervals for binomial proportions; pair-level intervals are descriptive because variants within queries are correlated");
        row(notes,"McNemar","Continuity-corrected chi-square, asymptotic p; query-level any-success comparison is preferred for independence");
        row(notes,"Pair-level p-values","Exploratory only: 6 variants within a query are dependent");
        row(notes,"No inference","These outcomes measure lexical/semantic coverage, not downstream retrieval relevance");
        for(int i=0;i<output.getNumberOfSheets();i++){
            Sheet s=output.getSheetAt(i);s.createFreezePane(0,1);
            int cols=Math.min(s.getRow(0).getLastCellNum(),20);for(int c=0;c<cols;c++)s.setColumnWidth(c,Math.min(10000,Math.max(3600,s.getRow(0).getCell(c).toString().length()*260)));
        }
        try(FileOutputStream f=new FileOutputStream(out)){output.write(f);}
        output.close();source.close();
        System.out.printf(Locale.US,"Queries=%d Pairs=%d Lexical=%.4f%% Semantic=%.4f%% Strict=%.4f%% Recovery=%.4f%% IntegrityIssues=%d%n",evaluated,total.n,pct(total.lex,total.n),pct(total.sem,total.n),pct(total.strict,total.n),pct(total.recovered,total.miss),errors);
        System.out.println("Created: "+out);
    }
    static int scoreCount(String s){double d=Double.parseDouble(s);if(d!=(int)d||d<0||d>6)throw new IllegalArgumentException();return (int)d;}
}
