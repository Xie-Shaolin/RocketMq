package com.lin;

/**
 * Hello world!
 *
 */
public class App 
{
    static int a=0;
    public static void main( String[] args ) throws InterruptedException {

        for ( int i = 0; i < 10000; i++) {

            new Thread(()->{
                System.out.println( "num:"+a );
                a++;
            }).start();
            if(a==10){
                System.out.println("---error---");
                int b = 1/0;
            }
        }


    }
}
