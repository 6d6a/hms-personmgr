package ru.majordomo.hms.personmgr;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

//class BoxPrinter<T> {
//    private T val;
//
//    public BoxPrinter(T arg) {
//        val = arg;
//    }
//
//    public String toString() {
//        return "{" + val + "}";
//    }
//
//    public T getValue() {
//        return val;
//    }
//}
//
//
//public class test {
//    public static void main(String[] args) {
////        BigDecimal a = BigDecimal.valueOf(0);
////        System.out.println(a.divide(BigDecimal.valueOf(13)));
////        List<Integer> l = new ArrayList<>();
////        l.add(1);
////        List<Integer> f = l.stream().filter(a -> a != 1).collect(Collectors.toList());
////        if (f == null || f.isEmpty()) {System.out.println("null");}
////        List<Integer> b = f.stream().filter(a -> a.equals(1)).collect(Collectors.toList());
////        BoxPrinter<Integer> value1 = new BoxPrinter<Integer>(new Integer(10));
////        System.out.println(value1);
////        Integer intValue1 = value1.getValue();
////        BoxPrinter<String> value2 = new BoxPrinter<String>("Hello world");
////        System.out.println(value2);
////
////        // Здесь повторяется ошибка предыдущего фрагмента кода
////        String intValue2 = value2.getValue();
//        int o = 134;
//        List<Object> intList = new ArrayList<>();
//        intList.add(o);
//
//    }
//}
class Test {
    static void printList(List<?> list) {
        for (Object l : list)
            System.out.println("{" + l + "}");
    }

    public static void main(String[] args) {
//        List<Integer> list = new ArrayList<>();
//        list.add(10);
//        list.add(100);
//        printList(list);
//        List<String> strList = new ArrayList<>();
//        strList.add("10");
//        strList.add("100");
//        printList(strList);
//        BigDecimal a = new BigDecimal("1.00");
//        System.out.println(a.compareTo(BigDecimal.ZERO) > 0);
//        System.out.println(LocalDate.now().plusDays(1).compareTo(LocalDate.now()));
//        System.out.println(daysBeforeExpired);
        /*BigDecimal a = BigDecimal.valueOf(1);
        a = a.add(BigDecimal.valueOf(1));
        System.out.println(a);*/
        /*LocalDate dateFinish = LocalDate.now();
        System.out.println(dateFinish.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));*/
        System.out.println(BigDecimal.valueOf(0.0000));
    }
}