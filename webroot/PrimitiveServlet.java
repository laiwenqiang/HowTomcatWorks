import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class PrimitiveServlet implements Servlet {

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("init");
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        System.out.println("from service");
        PrintWriter out = servletResponse.getWriter();

        //由于浏览器无法解析出相应，故添加如下一段代码。原因未知。
        String msg = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n";
        out.write(msg);

        out.print("Hello. Rose are red.");
        out.print("Violets are blue.");

        out.close();
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
        System.out.println("destroy");
    }
}
