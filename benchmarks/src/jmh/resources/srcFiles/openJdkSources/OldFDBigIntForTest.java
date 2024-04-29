/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * A really, really simple bigint package
 * tailored to the needs of floating base conversion.
 */
class OldFDBigIntForTest {
    int nWords; 
    int data[]; 


    public OldFDBigIntForTest( int v ){
        nWords = 1;
        data = new int[1];
        data[0] = v;
    }

    public OldFDBigIntForTest( long v ){
        data = new int[2];
        data[0] = (int)v;
        data[1] = (int)(v>>>32);
        nWords = (data[1]==0) ? 1 : 2;
    }

    public OldFDBigIntForTest( OldFDBigIntForTest other ){
        data = new int[nWords = other.nWords];
        System.arraycopy( other.data, 0, data, 0, nWords );
    }

    private OldFDBigIntForTest( int [] d, int n ){
        data = d;
        nWords = n;
    }

    public OldFDBigIntForTest( long seed, char digit[], int nd0, int nd ){
        int n= (nd+8)/9;        
        if ( n < 2 ) n = 2;
        data = new int[n];      
        data[0] = (int)seed;    
        data[1] = (int)(seed>>>32);
        nWords = (data[1]==0) ? 1 : 2;
        int i = nd0;
        int limit = nd-5;       
        int v;
        while ( i < limit ){
            int ilim = i+5;
            v = (int)digit[i++]-(int)'0';
            while( i <ilim ){
                v = 10*v + (int)digit[i++]-(int)'0';
            }
            multaddMe( 100000, v); 
        }
        int factor = 1;
        v = 0;
        while ( i < nd ){
            v = 10*v + (int)digit[i++]-(int)'0';
            factor *= 10;
        }
        if ( factor != 1 ){
            multaddMe( factor, v );
        }
    }

    /*
     * Left shift by c bits.
     * Shifts this in place.
     */
    public void
    lshiftMe( int c )throws IllegalArgumentException {
        if ( c <= 0 ){
            if ( c == 0 )
                return; 
            else
                throw new IllegalArgumentException("negative shift count");
        }
        int wordcount = c>>5;
        int bitcount  = c & 0x1f;
        int anticount = 32-bitcount;
        int t[] = data;
        int s[] = data;
        if ( nWords+wordcount+1 > t.length ){
            t = new int[ nWords+wordcount+1 ];
        }
        int target = nWords+wordcount;
        int src    = nWords-1;
        if ( bitcount == 0 ){
            System.arraycopy( s, 0, t, wordcount, nWords );
            target = wordcount-1;
        } else {
            t[target--] = s[src]>>>anticount;
            while ( src >= 1 ){
                t[target--] = (s[src]<<bitcount) | (s[--src]>>>anticount);
            }
            t[target--] = s[src]<<bitcount;
        }
        while( target >= 0 ){
            t[target--] = 0;
        }
        data = t;
        nWords += wordcount + 1;
        while ( nWords > 1 && data[nWords-1] == 0 )
            nWords--;
    }

    /*
     * normalize this number by shifting until
     * the MSB of the number is at 0x08000000.
     * This is in preparation for quoRemIteration, below.
     * The idea is that, to make division easier, we want the
     * divisor to be "normalized" -- usually this means shifting
     * the MSB into the high words sign bit. But because we know that
     * the quotient will be 0 < q < 10, we would like to arrange that
     * the dividend not span up into another word of precision.
     * (This needs to be explained more clearly!)
     */
    public int
    normalizeMe() throws IllegalArgumentException {
        int src;
        int wordcount = 0;
        int bitcount  = 0;
        int v = 0;
        for ( src= nWords-1 ; src >= 0 && (v=data[src]) == 0 ; src--){
            wordcount += 1;
        }
        if ( src < 0 ){
            throw new IllegalArgumentException("zero value");
        }
        /*
         * In most cases, we assume that wordcount is zero. This only
         * makes sense, as we try not to maintain any high-order
         * words full of zeros. In fact, if there are zeros, we will
         * simply SHORTEN our number at this point. Watch closely...
         */
        nWords -= wordcount;
        /*
         * Compute how far left we have to shift v s.t. its highest-
         * order bit is in the right place. Then call lshiftMe to
         * do the work.
         */
        if ( (v & 0xf0000000) != 0 ){
            for( bitcount = 32 ; (v & 0xf0000000) != 0 ; bitcount-- )
                v >>>= 1;
        } else {
            while ( v <= 0x000fffff ){
                v <<= 8;
                bitcount += 8;
            }
            while ( v <= 0x07ffffff ){
                v <<= 1;
                bitcount += 1;
            }
        }
        if ( bitcount != 0 )
            lshiftMe( bitcount );
        return bitcount;
    }

    /*
     * Multiply a OldFDBigIntForTest by an int.
     * Result is a new OldFDBigIntForTest.
     */
    public OldFDBigIntForTest
    mult( int iv ) {
        long v = iv;
        int r[];
        long p;

        r = new int[ ( v * ((long)data[nWords-1]&0xffffffffL) > 0xfffffffL ) ? nWords+1 : nWords ];
        p = 0L;
        for( int i=0; i < nWords; i++ ) {
            p += v * ((long)data[i]&0xffffffffL);
            r[i] = (int)p;
            p >>>= 32;
        }
        if ( p == 0L){
            return new OldFDBigIntForTest( r, nWords );
        } else {
            r[nWords] = (int)p;
            return new OldFDBigIntForTest( r, nWords+1 );
        }
    }

    /*
     * Multiply a OldFDBigIntForTest by an int and add another int.
     * Result is computed in place.
     * Hope it fits!
     */
    public void
    multaddMe( int iv, int addend ) {
        long v = iv;
        long p;

        p = v * ((long)data[0]&0xffffffffL) + ((long)addend&0xffffffffL);
        data[0] = (int)p;
        p >>>= 32;
        for( int i=1; i < nWords; i++ ) {
            p += v * ((long)data[i]&0xffffffffL);
            data[i] = (int)p;
            p >>>= 32;
        }
        if ( p != 0L){
            data[nWords] = (int)p; 
            nWords++;
        }
    }

    /*
     * Multiply a OldFDBigIntForTest by another OldFDBigIntForTest.
     * Result is a new OldFDBigIntForTest.
     */
    public OldFDBigIntForTest
    mult( OldFDBigIntForTest other ){
        int r[] = new int[ nWords + other.nWords ];
        int i;

        for( i = 0; i < this.nWords; i++ ){
            long v = (long)this.data[i] & 0xffffffffL; 
            long p = 0L;
            int j;
            for( j = 0; j < other.nWords; j++ ){
                p += ((long)r[i+j]&0xffffffffL) + v*((long)other.data[j]&0xffffffffL); 
                r[i+j] = (int)p;
                p >>>= 32;
            }
            r[i+j] = (int)p;
        }
        for ( i = r.length-1; i> 0; i--)
            if ( r[i] != 0 )
                break;
        return new OldFDBigIntForTest( r, i+1 );
    }

    /*
     * Add one OldFDBigIntForTest to another. Return a OldFDBigIntForTest
     */
    public OldFDBigIntForTest
    add( OldFDBigIntForTest other ){
        int i;
        int a[], b[];
        int n, m;
        long c = 0L;
        if ( this.nWords >= other.nWords ){
            a = this.data;
            n = this.nWords;
            b = other.data;
            m = other.nWords;
        } else {
            a = other.data;
            n = other.nWords;
            b = this.data;
            m = this.nWords;
        }
        int r[] = new int[ n ];
        for ( i = 0; i < n; i++ ){
            c += (long)a[i] & 0xffffffffL;
            if ( i < m ){
                c += (long)b[i] & 0xffffffffL;
            }
            r[i] = (int) c;
            c >>= 32; 
        }
        if ( c != 0L ){
            int s[] = new int[ r.length+1 ];
            System.arraycopy( r, 0, s, 0, r.length );
            s[i++] = (int)c;
            return new OldFDBigIntForTest( s, i );
        }
        return new OldFDBigIntForTest( r, i );
    }

    /*
     * Subtract one OldFDBigIntForTest from another. Return a OldFDBigIntForTest
     * Assert that the result is positive.
     */
    public OldFDBigIntForTest
    sub( OldFDBigIntForTest other ){
        int r[] = new int[ this.nWords ];
        int i;
        int n = this.nWords;
        int m = other.nWords;
        int nzeros = 0;
        long c = 0L;
        for ( i = 0; i < n; i++ ){
            c += (long)this.data[i] & 0xffffffffL;
            if ( i < m ){
                c -= (long)other.data[i] & 0xffffffffL;
            }
            if ( ( r[i] = (int) c ) == 0 )
                nzeros++;
            else
                nzeros = 0;
            c >>= 32; 
        }
        assert c == 0L : c; 
        assert dataInRangeIsZero(i, m, other); 
        return new OldFDBigIntForTest( r, n-nzeros );
    }

    private static boolean dataInRangeIsZero(int i, int m, OldFDBigIntForTest other) {
        while ( i < m )
            if (other.data[i++] != 0)
                return false;
        return true;
    }

    /*
     * Compare OldFDBigIntForTest with another OldFDBigIntForTest. Return an integer
     * >0: this > other
     *  0: this == other
     * <0: this < other
     */
    public int
    cmp( OldFDBigIntForTest other ){
        int i;
        if ( this.nWords > other.nWords ){
            int j = other.nWords-1;
            for ( i = this.nWords-1; i > j ; i-- )
                if ( this.data[i] != 0 ) return 1;
        }else if ( this.nWords < other.nWords ){
            int j = this.nWords-1;
            for ( i = other.nWords-1; i > j ; i-- )
                if ( other.data[i] != 0 ) return -1;
        } else{
            i = this.nWords-1;
        }
        for ( ; i > 0 ; i-- )
            if ( this.data[i] != other.data[i] )
                break;
        int a = this.data[i];
        int b = other.data[i];
        if ( a < 0 ){
            if ( b < 0 ){
                return a-b; 
            } else {
                return 1; 
            }
        } else {
            if ( b < 0 ) {
                return -1;
            } else {
                return a - b;
            }
        }
    }

    /*
     * Compute
     * q = (int)( this / S )
     * this = 10 * ( this mod S )
     * Return q.
     * This is the iteration step of digit development for output.
     * We assume that S has been normalized, as above, and that
     * "this" has been lshift'ed accordingly.
     * Also assume, of course, that the result, q, can be expressed
     * as an integer, 0 <= q < 10.
     */
    public int
    quoRemIteration( OldFDBigIntForTest S )throws IllegalArgumentException {
        if ( nWords != S.nWords ){
            throw new IllegalArgumentException("disparate values");
        }
        int n = nWords-1;
        long q = ((long)data[n]&0xffffffffL) / (long)S.data[n];
        long diff = 0L;
        for ( int i = 0; i <= n ; i++ ){
            diff += ((long)data[i]&0xffffffffL) -  q*((long)S.data[i]&0xffffffffL);
            data[i] = (int)diff;
            diff >>= 32; 
        }
        if ( diff != 0L ) {
            long sum = 0L;
            while ( sum ==  0L ){
                sum = 0L;
                for ( int i = 0; i <= n; i++ ){
                    sum += ((long)data[i]&0xffffffffL) +  ((long)S.data[i]&0xffffffffL);
                    data[i] = (int) sum;
                    sum >>= 32; 
                }
                /*
                 * Originally the following line read
                 * "if ( sum !=0 && sum != -1 )"
                 * but that would be wrong, because of the
                 * treatment of the two values as entirely unsigned,
                 * it would be impossible for a carry-out to be interpreted
                 * as -1 -- it would have to be a single-bit carry-out, or
                 * +1.
                 */
                assert sum == 0 || sum == 1 : sum; 
                q -= 1;
            }
        }
        long p = 0L;
        for ( int i = 0; i <= n; i++ ){
            p += 10*((long)data[i]&0xffffffffL);
            data[i] = (int)p;
            p >>= 32; 
        }
        assert p == 0L : p; 
        return (int)q;
    }

    public long
    longValue(){
        assert this.nWords > 0 : this.nWords; 

        if (this.nWords == 1)
            return ((long)data[0]&0xffffffffL);

        assert dataInRangeIsZero(2, this.nWords, this); 
        assert data[1] >= 0;  
        return ((long)(data[1]) << 32) | ((long)data[0]&0xffffffffL);
    }

    public String
    toString() {
        StringBuffer r = new StringBuffer(30);
        r.append('[');
        int i = Math.min( nWords-1, data.length-1) ;
        if ( nWords > data.length ){
            r.append( "("+data.length+"<"+nWords+"!)" );
        }
        for( ; i> 0 ; i-- ){
            r.append( Integer.toHexString( data[i] ) );
            r.append(' ');
        }
        r.append( Integer.toHexString( data[0] ) );
        r.append(']');
        return new String( r );
    }
}
