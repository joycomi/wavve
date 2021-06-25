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
    @Autowired VideoRepository videoRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_ModifyStatus(@Payload Paid paid){

        if(!paid.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + paid.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(paid.getVideoId());
        Video video = videoOptional.get();
        
        //video.setVideoId(paid.getVideoId());
        //video.setStatus("BOOKED");
        video.setStatus(paid.getStatus());
        videoRepository.save(video);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookingCancelled_ModifyStatus(@Payload BookingCancelled bookingCancelled){

        if(!bookingCancelled.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + bookingCancelled.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(bookingCancelled.getVideoId());
        Video video = videoOptional.get();
        
       // video.setVideoId(bookingCancelled.getVideoId());
        video.setStatus(bookingCancelled.getStatus());

        videoRepository.save(video);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverVideoRented_ModifyStatus(@Payload VideoRented videoRented){

        if(!videoRented.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + videoRented.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(videoRented.getVideoId());
        Video video = videoOptional.get();
        
       // video.setVideoId(videoRented.getVideoId());
        video.setStatus(videoRented.getStatus());

        videoRepository.save(video);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverVideoReturned_ModifyStatus(@Payload VideoReturned videoReturned){

        if(!videoReturned.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + videoReturned.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(videoReturned.getVideoId());
        Video video = videoOptional.get();
        
       // video.setVideoId(bookingCancelled.getVideoId());
        video.setStatus(videoReturned.getStatus());

        videoRepository.save(video);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
