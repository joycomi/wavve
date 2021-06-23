package video;

import video.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{
    @Autowired RentalRepository rentalRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverStatusModified_ModifyStatus(@Payload StatusModified statusModified){

        if(!statusModified.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(rental) : " + statusModified.toJson() + "\n\n");

        Iterable<Rental> rentals= rentalRepository.findAll();

        for (Rental rental : rentals) {
            //System.out.println("\n\n##### listener UpdateStatus-bookings-for #### \n\n");
            if(rental.getVideoId().equals(statusModified.getVideoId()))
            {
                rental.setStatus(statusModified.getStatus());
                rentalRepository.save(rental);

                break;
            }
        }
           
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
