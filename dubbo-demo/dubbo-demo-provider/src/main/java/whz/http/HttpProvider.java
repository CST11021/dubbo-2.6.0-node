package whz.http;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Author: wanghz
 * @Date: 2020/5/2 5:29 PM
 */
public class HttpProvider {

    public static void main(String[] args) throws Exception {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/http/dubbo-demo-provider.xml"});
        context.start();

        System.in.read(); // press any key to exit
    }

}
