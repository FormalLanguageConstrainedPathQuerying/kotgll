/*
 * @test /nodynamiccopyright/
 * @bug 8194743
 * @summary Permit additional statements before this/super in constructors
 * @compile/fail/ref=SuperInitFails.out -XDrawDiagnostics SuperInitFails.java
 * @enablePreview
 */
import java.util.concurrent.atomic.AtomicReference;
public class SuperInitFails extends AtomicReference<Object> implements Iterable<Object> {

    private int x;


    public SuperInitFails() {           
    }

    public SuperInitFails(Object x) {
        this.x = x.hashCode();          
    }

    public SuperInitFails(byte x) {
        super();                        
    }

    public SuperInitFails(char x) {
        this((int)x);                   
    }


    {
        this(1);                        
    }

    {
        super();                        
    }

    void normalMethod1() {
        super();                        
    }

    void normalMethod2() {
        this();                         
    }

    void normalMethod3() {
        Runnable r = () -> super();     
    }

    void normalMethod4() {
        Runnable r = () -> this();      
    }

    public SuperInitFails(short x) {
        hashCode();                     
        super();
    }

    public SuperInitFails(float x) {
        this.hashCode();                
        super();
    }

    public SuperInitFails(int x) {
        super.hashCode();               
        super();
    }

    public SuperInitFails(long x) {
        SuperInitFails.this.hashCode();      
        super();
    }

    public SuperInitFails(double x) {
        SuperInitFails.super.hashCode();     
        super();
    }

    public SuperInitFails(byte[] x) {
        {
            super();                    
        }
    }

    public SuperInitFails(char[] x) {
        if (x.length == 0)
            return;                     
        super();
    }

    public SuperInitFails(short[] x) {
        this.x = x.length;              
        super();
    }

    public SuperInitFails(float[] x) {
        System.identityHashCode(this);  
        super();
    }

    public SuperInitFails(int[] x) {
        this(this);                     
    }

    public SuperInitFails(long[] x) {
        this(Object.this);              
    }

    public SuperInitFails(double[] x) {
        Iterable.super.spliterator();   
        super();
    }

    public SuperInitFails(byte[][] x) {
        super(new Object() {
            {
                super();                
            }
        });
    }

    public SuperInitFails(char[][] x) {
        new Inner1();                   
        super();
    }

    class Inner1 {
    }

    record Record1(int value) {
        Record1(float x) {              
        }
    }

    record Record2(int value) {
        Record2(float x) {              
            super();
        }
    }

    @Override
    public java.util.Iterator<Object> iterator() {
        return null;
    }

    public SuperInitFails(short[][] x) {
        class Foo {
            Foo() {
                SuperInitFails.this.hashCode();
            }
        };
        new Foo();                      
        super();
    }

    public SuperInitFails(float[][] x) {
        Runnable r = () -> {
            super();                    
        };
    }

    public SuperInitFails(int[][] z) {
        super((Runnable)() -> x);       
    }

    public SuperInitFails(long[][] z) {
        super(new Inner1());            
    }
}
