package video;

import video.config.kafka.KafkaProcessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{
    @Autowired PayRepository payRepository;
    @Autowired RefundRepository refundRepository;

    @Value("${log.refund.path}")
    String PATH;

    @Value("${log.refund.directory}")
    String directoryName;

    @Value("${log.refund.file}")
    String file;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookingCancelled_Refund(@Payload BookingCancelled bookingCancelled){

        if(!bookingCancelled.validate()) return;

        System.out.println("\n\n##### listener Refund(pay) : " + bookingCancelled.toJson() + "\n\n");

        //Pay pay = new Pay();
        Iterable<Pay> pays= payRepository.findAll();

        for (Pay pay : pays) {
            //System.out.println("\n\n##### listener UpdateStatus-bookings-for #### \n\n");
            if(pay.getRentId().equals(bookingCancelled.getRentId()))
            {
                pay.setPrice(bookingCancelled.getRentPrice());
                pay.setPayStatus(bookingCancelled.getStatus());
                pay.setRentId(bookingCancelled.getRentId());
                pay.setVideoId(bookingCancelled.getVideoId());
        
                payRepository.save(pay);

                break;
            }
        }

        //Refund refund = new Refund();
        //refundRepository.save(refund);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRefunded_RegRefund(@Payload Refunded refunded) throws IOException {
        //환불내역 저장
        if(!refunded.validate()) return;

        System.out.println("\n\n##### listener RegRefund(pay) : " + refunded.toJson() + "\n\n");

        //Pay pay = new Pay();
        //payRepository.save(pay);

        Refund refund = new Refund();

        refund.setPayId(refunded.getPayId());
        refund.setPrice(refunded.getPrice());
        refund.setPayStatus("Refunded");
        refund.setRentId(refunded.getRentId());

        refundRepository.save(refund);
        
        
        //환불내역 파일 기록
        String str = refunded.toJson()+"\n";
        // String PATH = "./";
        // String directoryName ="logs";
        // String fileName = PATH+directoryName+"/"+"refunded.log";

        String fileName = PATH+directoryName+"/"+file;
        
        File file  = new File(String.valueOf(fileName));
        File directory = new File(String.valueOf(directoryName));
        
        if(!directory.exists()){
        
            directory.mkdir();
            //if(!file.exists() && !checkEnoughDiskSpace()){
            if(!file.exists()){
                file.getParentFile().mkdir();
                file.createNewFile();
            }
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(str);
        bw.close();
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
