/*
 * @test /nodynamiccopyright/
 * @bug 8039026
 * @summary Definitely unassigned field can be accessed
 * @compile/fail/ref=T8039026.out -XDrawDiagnostics T8039026.java
 */

public class T8039026 {
    final int x,y,z;
    final int a = this.y;  
    {
        int b = true ? this.x : 0;  
        System.out.println(this.x); 
        this.y = 1;
    }
    T8039026() {
        this.x = 1;      
        this.y = 1;      
        this.z = this.x; 
    }
}
