/* /nodynamiccopyright/ */

package java.lang;

@jdk.internal.ValueBased
public final class SomeVbc {

    public SomeVbc() {}

    final String ref = "String";

    void abuseVbc() {

        synchronized(ref) {           
            synchronized (this) {     
            }
        }
    }
}

final class AuxilliaryAbuseOfVbc {

    void abuseVbc(SomeVbc vbc) {

        synchronized(this) {           
            synchronized (vbc) {       
            }
        }
    }
}

