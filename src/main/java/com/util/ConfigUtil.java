package com.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * 配置文件工具类
 * Created by jianghongkai on 2015/1/17.
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    private static Properties props=new Properties();
    public static void load(String propUrl) {
        logger.debug("配置文件路径：{}",propUrl);
        ClassLoader loader=ConfigUtil.class.getClassLoader();
        InputStream ips=null;
        try {
            ips=loader.getResourceAsStream(propUrl);
            BufferedReader bf = new BufferedReader(new InputStreamReader(ips,"GBK"));
            props.load(bf);
        } catch (IOException e) {
            logger.error("读取配置文件出错！原因："+e.getMessage(),e);
        }finally {
            try {
                if(ips!=null)
                    ips.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static String getProperty(String key){
        return props.getProperty(key);
    }
}
