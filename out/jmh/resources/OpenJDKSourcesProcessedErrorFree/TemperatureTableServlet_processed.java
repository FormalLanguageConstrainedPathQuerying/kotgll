import java.io.*;
import java.util.*;

public class TemperatureTableServlet /* extends HttpServlet */{
        public static void main(String[] args) {
                HelloWorld.main(args); 
                PrintStream out = System.out;
                out.println("<html>");
                out.println("<head>");
                out.println("   <title>Temperature Table</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("");
                out.println("<h1>Temperature Table</h1>");
                out.println("<p>American tourists visiting Canada can use this handy temperature");
                out.println("table which converts from Fahrenheit to Celsius:");
                out.println("<br><br>");
                out.println("");
                out.println("<table BORDER COLS=2 WIDTH=\"20%\" >");
                out.println("<tr BGCOLOR=\"#FFFF00\">");
                out.println("<th>Fahrenheit</th>");
                out.println("<th>Celsius</th>");
                out.println("</tr>");
                out.println("");
                for (int i = 0; i <= 100; i += 10) {
                        out.println("<tr ALIGN=RIGHT BGCOLOR=\"#CCCCCC\">");
                        out.println("<td>" + i + "</td>");
                        out.println("<td>" + ((i - 32) * 5 / 9) + "</td>");
                        out.println("</tr>");
                }
                out.println("");
                out.println("</table>");
                out.println("<p><i>Created " + new Date () + "</i></p>");
                out.println("");
                out.println("</body>");
                out.println("</html>");
        }
}
