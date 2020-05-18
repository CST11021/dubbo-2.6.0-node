package whz.stub;

import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.demo.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

    public static void main(String[] args) {

        Wrapper wrapper = Wrapper.getWrapper(DemoService.class);


        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService");

        while (true) {
            try {
                Thread.sleep(1000);
                String hello1 = demoService.sayHello("李四");
                System.out.println(hello1);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }
}