package video;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Rental_table")
public class Rental {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer rentId;
    private Integer videoId;
    private String videoTitle;
    private Integer rentPrice;
    private String status;
    private String memId;

    @PostPersist
    public void onPostPersist(){
        //예약&결제정보 전달

        VideoBooked videoBooked = new VideoBooked();
        BeanUtils.copyProperties(this, videoBooked);
        videoBooked.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        video.external.Pay pay = new video.external.Pay();
        pay.setRentId(this.getRentId());
        pay.setPrice(this.getRentPrice());
        pay.setPayStatus(this.getStatus()); //OK, NotOK
        pay.setVideoId(this.getVideoId());

        //boolean rslt = false;
        // mappings goes here
         RentalApplication.applicationContext.getBean(video.external.PayService.class)
            .payment(pay);

        }

    @PostUpdate
    public void onPostUpdate(){
        System.out.println("\n\n##### listener PreUpdate(Rental) : " + this.getRentId().toString()+": "+this.getStatus().toString() + "\n\n");

        // 예약취소, 대여, 반납 처리 시 이벤트 발생
        if("CANCEL".equals(this.getStatus())){
            BookingCancelled bookingCancelled = new BookingCancelled();
            this.setStatus("CANCELLED");
            BeanUtils.copyProperties(this, bookingCancelled);
            bookingCancelled.publishAfterCommit();

        }else if("RENT".equals(this.getStatus())){
            VideoRented videoRented = new VideoRented();
            this.setStatus("RENTED");
            BeanUtils.copyProperties(this, videoRented);
            videoRented.publishAfterCommit();
        
        }else if("RETURN".equals(this.getStatus())){
            VideoReturned videoReturned = new VideoReturned();
            this.setStatus("RETURNED");
            BeanUtils.copyProperties(this, videoReturned);
            videoReturned.publishAfterCommit();
        }

    }


    public Integer getRentId() {
        return rentId;
    }

    public void setRentId(Integer rentId) {
        this.rentId = rentId;
    }
    public Integer getVideoId() {
        return videoId;
    }

    public void setVideoId(Integer videoId) {
        this.videoId = videoId;
    }
    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }
    public Integer getRentPrice() {
        return rentPrice;
    }

    public void setRentPrice(Integer rentPrice) {
        this.rentPrice = rentPrice;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getMemId() {
        return memId;
    }

    public void setMemId(String memId) {
        this.memId = memId;
    }




}
