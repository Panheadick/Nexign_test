package org.example;

import java.io.*;
import java.text.*;
import java.util.*;
import java.time.*;

class Subscriber {
    private String number; // номер абонента
    ArrayList<Call> calls = new ArrayList<Call>();//список всех звонков абонента

    public Subscriber(String number, Call call) {
        this.number = number;
        calls.add(call);
    }

    public String getNumber() {
        return number;
    }

    public ArrayList<Call> getCalls() {
        return calls;
    }

    public void addCall(Call call) { //Добавить звонок в список абонента
        calls.add(call);
    }

    //Создать файл отчета
    public void createReport() throws Exception {
        String filePath = "reports/report number " + number + ".txt";// Файл в названии которого будет номер
        BufferedWriter reportFile = new BufferedWriter(new FileWriter(filePath));

        reportFile.write("Tariff index: " + calls.get(0).getTariff() + "\n");
        reportFile.write("----------------------------------------------------------------------------\n");
        reportFile.write("Report for phone number " + number + ":\n");
        reportFile.write("----------------------------------------------------------------------------\n");
        reportFile.write("| Call Type |   Start Time        |     End Time        | Duration | Cost  |\n");

        //Заполнение таблицы звонками
        long totalMin = 0;//Общее время разговоров
        double totalPrice = 0;//Сумма стоимость разговоров
        for (Call call : calls) {
            //Форматирование даты и времени
            DateFormat originalFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            DateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Date dateStart = originalFormat.parse(call.getStartTime());
            String start = targetFormat.format(dateStart);
            Date dateEnd = originalFormat.parse(call.getEndTime());
            String end = targetFormat.format(dateEnd);
            //Нахождение длительности разговора
            long durabilityMiliSec = dateEnd.getTime() - dateStart.getTime();
            long second = (durabilityMiliSec / 1000) % 60;
            long minutes = (durabilityMiliSec / (1000 * 60)) % 60;
            long hour = (durabilityMiliSec / (1000 * 60 * 60)) % 24;
            String durability = String.format("%02d:%02d:%02d", hour, minutes, second);
            //Нахождение стоимости разговора
            double price = 0;
            //Для тарифа "Поминутный"
            minutes = minutes + hour * 60 + 1;
            if ("03".equals(call.getTariff())) {
                price = minutes * 1.5;
                //Для тарифа "Обычный"
            } else if ("11".equals(call.getTariff())) {
                if ("02".equals(call.getTypeCall())) {//входящий звонок
                    price = 0;
                } else if ("01".equals(call.getTypeCall())) {//исходящий звонок
                    if (totalMin + minutes >= 100) {//Звонок после более 100 потраченных минут
                        price = minutes  * 0.5;
                    } else if (totalMin - minutes >= 100) {//Звонок начался до 100 потраченных минут и закончился после
                        price = (100 - minutes) * 0.5 + (totalMin + minutes - 100) * 1.5;
                    } else {//Звонок начался и закончился до 100 потраченных минут
                        price = minutes * 0.5;
                    }
                }
                //Для тарифа "Безлимитный"
            } else if ("06".equals(call.getTariff())) {
                if (totalMin + minutes > 300) {//Звонок после более 300 потраченных минут
                    price = minutes;
                } else if (totalMin - minutes > 300) {//Звонок начался до 300 потраченных минут и закончился после
                    price =  (totalMin + minutes - 300);
                } else {//Звонок начался и закончился до 300 потраченных минут
                    price = 0;
                }
            }

            totalMin += minutes;
            totalPrice += price;

            reportFile.write("|     " + call.getTypeCall() + "    |");
            reportFile.write(" " + start + " |");
            reportFile.write(" " + end + " |");
            reportFile.write(" " + durability + " |");
            reportFile.write("" + String.format("%6.2f", price) + " |\n");

        }
        reportFile.write("----------------------------------------------------------------------------\n");
        reportFile.write("|                                           Total Cost: |"
                +  String.format("%10.2f", totalPrice) +" rubles |\n");
        reportFile.write("----------------------------------------------------------------------------");
        reportFile.close();
    }
}

class Call {
    private String typeCall; // тип вызова
    private String startTime; // дата и время начала звонка
    private String endTime; // дата и время конца звонка
    private String tariff; // тариф

    public Call(String typeCall, String startTime, String endTime, String tariff) {
        this.typeCall = typeCall;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tariff = tariff;
    }

    public String getTypeCall() {
        return typeCall;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getTariff() {
        return tariff;
    }
}

public class Main {
    public static void main(String[] args) throws Exception {
        FileReader fr = new FileReader("src/main/resources/cdr.txt");
        BufferedReader reader = new BufferedReader(fr);
        String line = reader.readLine();

        ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();

        while (line != null) {
            String[] parts = line.split(", ");
            Call call = new Call(parts[0], parts[2], parts[3], parts[4]);
            boolean flag = true;
            for (Subscriber i : subscribers) {
                if (i.getNumber().equals(parts[1])) {
                    i.addCall(call);
                    flag = false;
                    break;
                }
            }
            if (flag) {
                Subscriber sub = new Subscriber(parts[1], call);
                subscribers.add(sub);
            }
            line = reader.readLine();
        }
        //Сортировка списка звонков по дате
        for (int i = 0; i < subscribers.size() - 1; i++) {
            for (int j = subscribers.get(i).getCalls().size() - 1; j >= 1; j--) {
                for (int k = 0; k < j; k++) {
                    if (Long.parseLong(subscribers.get(i).getCalls().get(k).getStartTime()) > Long.parseLong(subscribers.get(i).getCalls().get(k + 1).getStartTime()))               //Если порядок элементов нарушен
                        Collections.swap(subscribers.get(i).getCalls(), k, k + 1);
                }
            }
        }

        //Создание отчетов для всех абонентов с помощью созданног метода
        for (Subscriber i : subscribers) {
            i.createReport();
        }
        reader.close();
    }
}
