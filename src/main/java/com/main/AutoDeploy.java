package com.main;

import com.util.ConfigUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 发版打包程序
 * Created by jianghongkai on 2015/1/17.
 */
public class AutoDeploy {
    private static final Logger logger = LoggerFactory.getLogger(AutoDeploy.class);
    private String desURL=ConfigUtil.getProperty("des.url");
    private String sellerURL=ConfigUtil.getProperty("seller.url");
    private String buyerURL=ConfigUtil.getProperty("buyer.url");
    private String sourceURL=ConfigUtil.getProperty("source.url");
    //文件格式
    private static String FILTER_PREFIX=".txt";
    //过滤卖家路径的标志
    private static String SELLER="seller";
    //过滤卖家路径的标志
    private static String BUYER="buyer";
    //txt文件包含文件计数
    private Map<String,Integer> txtCount;
    //打包的文件计数
    private Map<String,Integer> filesCount;
    public AutoDeploy(){
       this.txtCount=new HashMap<String,Integer>();
       this.filesCount=new HashMap<String,Integer>();
    }

    /**
     * 打包
     */
    public void cellect(){
        //路径检查
        checkAllURL();
        //查找所有的txt文件
        File[] files=new File(sourceURL).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.indexOf(FILTER_PREFIX)>-1)
                    return true;
                return false;
            }
        });
        //开始循环处理txt
        for(File txt:files){
            logger.debug("开始处理文件：{}", txt.getName());
            //记录txt文件中记录数
            txtCount.put(txt.getName(), 0);
            try {
                BufferedReader br=new BufferedReader(new FileReader(txt));
                String lineTxt =null;
                while ((lineTxt =br.readLine())!=null){
                    //过滤空字符串
                    if(lineTxt.trim().equals(""))
                        continue;
                    logger.debug("准备处理文件：{}中的{}", txt.getName(), lineTxt);
                    String fileTemp=lineTxt;
                    //组装绝对路径
                    if(lineTxt.toLowerCase().indexOf(SELLER)>-1){
                        fileTemp=sellerURL.endsWith(File.separator)?sellerURL:(sellerURL+File.separator)+lineTxt;
                    }else if(lineTxt.toLowerCase().indexOf(BUYER)>-1){
                        fileTemp=buyerURL.endsWith(File.separator)?buyerURL:(buyerURL+File.separator)+lineTxt;
                    }else {
                        logger.error("{}中的[{}]路径错误！",txt.getName(),lineTxt);
                        System.exit(0);
                    }
                    logger.debug("要提取的文件路径：{}",fileTemp);
                    //文件打包
                    File ff=new File(fileTemp);
                    if(!ff.getParentFile().exists()||!ff.exists()){
                        logger.error("{}中的[{}]路径错误或文件不存在！",txt.getName(),lineTxt);
                        System.exit(0);
                    }else{
                        //记录txt文件中记录数
                        txtCount.put(txt.getName(),txtCount.get(txt.getName())+1);
                        //记录拷贝的文件数
                        if(filesCount.get(fileTemp)==null){
                            filesCount.put(fileTemp,1);
                            copy(desURL.endsWith(File.separator)?desURL:(desURL+File.separator)+lineTxt,ff);
                        }else{
                            filesCount.put(fileTemp,filesCount.get(fileTemp)+1);
                        }
                    }
                }
            }catch (Exception e){
                logger.error("错误："+e.getMessage(),e);
                System.exit(0);
            }
        }
    }
    private void checkAllURL(){
        //判断路径配置是否合法
        File seller= new File(sellerURL);
        File buyer= new File(buyerURL);
        File des= new File(desURL);
        File source = new File(sourceURL);
        boolean flag=true;
        if(!seller.isDirectory() || !seller.exists()){
            flag=false;
            logger.error("卖家路径错误！请检查：{}",sellerURL);
        }
        if(!buyer.isDirectory() || !buyer.exists()){
            flag=false;
            logger.error("买家路径错误！请检查：{}",buyerURL);
        }
        if(!source.isDirectory()|| !source.exists()) {
            flag=false;
            logger.error("源路径错误！请检查：{}",sourceURL);
        }
        if(!des.exists() && !des.mkdirs()){
            logger.error("无权创建目标路径！请检查：{}",desURL);
            System.exit(0);
        }
        if(!flag)
            System.exit(0);
    }
    /**
     * 写出结果到文件
     */
    private void writeResult(){
        //配置文件中读取目标路径
        logger.debug("配置文件中读取目标路径:{}",desURL);
        //组装要保存的文件：暂时固定为：bugfix.txt
        File file=new File(desURL.endsWith(File.separator)?desURL:(desURL+File.separator),"bugfix.txt");
        logger.debug("将要写入处理结果的文件:{}",desURL);
        if(file.exists()){
            file.delete();
        }
        //组装要输出的结果
        StringBuffer result=new StringBuffer("BUG fix:"+System.getProperty("line.separator"));
        if(this.txtCount==null || this.txtCount.size()==0){
            result.append("未找到txt文件!");
        }else{
            Iterator<String> it=txtCount.keySet().iterator();
            int i=1;
            int sum=0;//发版文件总数
            while(it.hasNext()){
                String url=it.next();
                int count=this.txtCount.get(url);
                sum=sum+count;
                result.append(i++).append(".").append(url)
                        .append("(文件数：").append(count).append(")")
                        .append(System.getProperty("line.separator"));
            }
            result.append("文件总数:")
                    .append(sum)
                    .append(",重复文件数：")
                    .append(sum-filesCount.size())
                    .append(",实际发版")
                    .append(filesCount.size());
        }
        logger.debug("bugfix.txt内容如下："+System.getProperty("line.separator")+result.toString());
        FileWriter fw =null;
        try {
            fw = new FileWriter(file);
            fw.write(result.toString());
        }catch (Exception e){
            logger.error("写入文件错误:" + e.getMessage(), e);
        }finally {
            //关闭流
            if(fw!=null){
                try {
                    fw.close();
                }catch (Exception e){
                    logger.error("关闭文件流失败:" + e.getMessage(),e);
                }
            }
        }
    }

    /**
     * 拷贝文件
     * @param desURL
     * @param source
     */
    private void copy(String desURL,File source){
        InputStream is =null;
        OutputStream os=null;
        try {
            is = new FileInputStream(source);
            //文件名字不变
            File desFile = new File(desURL, source.getName());
            File parentDir = desFile.getParentFile();
            //判断目录是否存在 ，不存在则创建
            if (!parentDir.exists() && !parentDir.mkdirs()){
                logger.error("无法创建目录：{}" , parentDir.getAbsolutePath());
                System.exit(0);
            }
            if(desFile.exists()){
                desFile.delete();
            }
            os  = new FileOutputStream(desFile);
            //开始拷贝
            IOUtils.copy(is, os);
        }catch (Exception e){
            logger.error("文件拷贝错误：{}", e.getMessage());
        }finally {
            //最终关闭流
            try {
                if (is != null)
                    is.close();
                if(os!=null)
                    os.close();
            }catch (Exception e){
                logger.error("关闭文件流失败：{}", e.getMessage());
            }
        }
    }
    public static void main(String[] args){
        String prop="config.properties";//(String )args[0];
        //加载配置文件
        ConfigUtil.load(prop);
        AutoDeploy autoDeploy =new AutoDeploy();
        //打包
        autoDeploy.cellect();
        //写出结果
        autoDeploy.writeResult();
    }
}
