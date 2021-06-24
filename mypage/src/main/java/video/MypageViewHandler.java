package video;

import video.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MypageViewHandler {

    @Autowired
    private MypageRepository mypageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenVideoBooked_then_CREATE_1 (@Payload VideoBooked videoBooked) {
        try {

            if (!videoBooked.validate()) return;

            // view 객체 생성
            Mypage mypage = new Mypage();
            // view 객체에 이벤트의 Value 를 set 함
            mypage.setRentId(videoBooked.getRentId());
            mypage.setVideoId(videoBooked.getVideoId());
            mypage.setVideoTitle(videoBooked.getVideoTitle());
            mypage.setRentPrice(videoBooked.getRentPrice());
            mypage.setStatus(videoBooked.getStatus());
            mypage.setMemId(videoBooked.getMemId());
            // view 레파지 토리에 save
            mypageRepository.save(mypage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenBookingCancelled_then_UPDATE_1(@Payload BookingCancelled bookingCancelled) {
        try {
            if (!bookingCancelled.validate()) return;
                // view 객체 조회
                Optional<Mypage> mypageOptional = mypageRepository.findById(bookingCancelled.getRentId());                

                if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setStatus(bookingCancelled.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenVideoRented_then_UPDATE_2(@Payload VideoRented videoRented) {
        try {
            if (!videoRented.validate()) return;
                // view 객체 조회
            Optional<Mypage> mypageOptional = mypageRepository.findById(videoRented.getRentId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setStatus(videoRented.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenVideoReturned_then_UPDATE_3(@Payload VideoReturned videoReturned) {
        try {
            if (!videoReturned.validate()) return;
                // view 객체 조회
            Optional<Mypage> mypageOptional = mypageRepository.findById(videoReturned.getRentId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setStatus(videoReturned.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}