/* /nodynamiccopyright/ */


class Q extends P {
    /** @deprecated */ public void pDep_qDep_rDep() { }
    /** @deprecated */ public void pDep_qDep_rUnd() { }
    /** @deprecated */ public void pDep_qDep_rInh() { }
                       public void pDep_qUnd_rDep() { } 
                       public void pDep_qUnd_rUnd() { } 
                       public void pDep_qUnd_rInh() { } 
    /** @deprecated */ public void pUnd_qDep_rDep() { }
    /** @deprecated */ public void pUnd_qDep_rUnd() { }
    /** @deprecated */ public void pUnd_qDep_rInh() { }
                       public void pUnd_qUnd_rDep() { }
                       public void pUnd_qUnd_rUnd() { }
                       public void pUnd_qUnd_rInh() { }
}
