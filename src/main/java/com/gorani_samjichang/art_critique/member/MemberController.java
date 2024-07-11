package com.gorani_samjichang.art_critique.member;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    final SNSMapRepository snsMapRepository;
    final MemberRepository memberRepository;
    @Value("${firebaseBucket}")
    String bucketName;

    /**
     * 먼저 데이터베이스에서 해당 이메일이 있는지를 뒤지고 있다면 아묻따 true 반환
     * 없다면 파이어베이스를 뒤져서 가입한 이메일이 있다면 그건 오류니까 파이어베이스에서 삭제 후  false반환
     * 이때 생각할 것이 탈퇴한 아이디의 email이라면 해당 email로 재가입이 가능하게 할지 여부 현재는 안되게 해놓음
     */
    @GetMapping("/emailCheck/{email}")
    boolean email_check(@PathVariable String email) {
        Optional<MemberEntity> memberEntity = memberRepository.findByEmail(email);
        if (memberEntity.isEmpty()) {
            try {
                UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
                FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
                return false;
            } catch (FirebaseAuthException ignored) {
                return false;
            }
        } else {
            return true;
        }
    }

    @GetMapping("/nicknameCheck/{nickname}")
    boolean nickname_check(@PathVariable String nickname) throws Exception {
        Optional<MemberEntity> memberEntity = memberRepository.findByNickname(nickname);
        return memberEntity.isPresent();
    }

    /**
     * created at은 파이어베이스가 이미 관리하고 있음, last_login 역시그럼
     * return 0=회원가입 성공, 1=회원정보 수정 성공
     */
    @PostMapping("/firebaseLoginTrial")
    int google_login_trial(@RequestParam(value = "uid") String uid,
                               @RequestParam(value = "nickname") String nickname,
                               @RequestParam(value = "level", required = false) String level,
                               @RequestParam(value = "profile_url", required = false) MultipartFile profile) throws FirebaseAuthException, IOException {
        Optional<MemberEntity> memberEntity = memberRepository.findBySnsMapKey(uid);
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        if(memberEntity.isPresent()) {
            System.out.println("회원정보 수정 절차를 시작합니다");
            String profile_url = null;
            if (profile != null) {
                profile_url = upload_to_storage(uid, profile);
            }
            memberEntity.get().setNickname(nickname);
            memberEntity.get().setProfile_url(profile_url);
            memberEntity.get().setLevel(level);
            memberRepository.save(memberEntity.get());
            return 1;
        }
        else {
            System.out.println("데이터베이스 회원가입 절차를 시작합니다");
            String profile_url = null;
            if (profile != null) {
                profile_url = upload_to_storage(uid, profile);
            }
            String email = userRecord.getEmail();
            MemberEntity me = MemberEntity
                    .builder()
                    .email(email)
                    .is_deleted(false)
                    .credit(0)
                    .nickname(nickname)
                    .profile_url(profile_url)
                    .level(level)
                    .build();
            SNSMapEntity sme = SNSMapEntity
                    .builder()
                    .key(uid)
                    .build();
            me.setSnsMap(sme);
            sme.setMember(me);
            memberRepository.save(me);
            return 0;
        }
    }

    @GetMapping("/levelList")
    String get_level_list() {
        return "[{\"value\": \"newbie\",\"display\": \"입문\",\"color\": \"rgb(245,125,125)\"},{\"value\": \"chobo\",\"display\": \"초보\",\"color\": \"rgb(214, 189, 81)\"},{\"value\": \"intermediate\",\"display\": \"중수\",\"color\": \"rgb(82, 227, 159)\"},{\"value\": \"gosu\",\"display\": \"고수\",\"color\": \"rgb(70, 104, 227)\"}]";
    }

    @GetMapping("/getAllMember")
    void get_all_member() {
        List<MemberEntity> list = memberRepository.findAll();
        for (MemberEntity me : list) {
            System.out.println(me.getNickname());
        }
    }

    @GetMapping("/isNewMember/{uid}")
    boolean is_new_member(@PathVariable("uid") String uid) {
        Optional<MemberEntity> memberEntity = memberRepository.findBySnsMapKey(uid);
        return memberEntity.isEmpty();
    }

    @GetMapping("/isMember/{email}")
    boolean is_member(@PathVariable("email") String email) {
        Optional<MemberEntity> memberEntity = memberRepository.findByEmail(email);
        return memberEntity.isPresent();
    }

    @GetMapping("/member/{uid}")
    MemberEntity member(@PathVariable("uid") String uid) {
        Optional<MemberEntity> memberEntity = memberRepository.findBySnsMapKey(uid);
        if (memberEntity.isPresent()) return memberEntity.get();
        else return null;
    }

    @GetMapping("/isDatabaseMember/{uid}")
    boolean is_database_member(@PathVariable("uid") String uid) throws FirebaseAuthException {
        System.out.println("데이터베이스에 있는지 검증 시작");
        Optional<MemberEntity> memberEntity = memberRepository.findBySnsMapKey(uid);
        if (memberEntity.isPresent()) {
            System.out.println("데이터베이스에 있음");
            return true;
        } else {
            System.out.println("데이터베이스에 없음");
            FirebaseAuth.getInstance().deleteUser(uid);
            return false;
        }
    }

    String upload_to_storage(String uid, MultipartFile profile) throws IOException {
        String fileName = uid;
        Bucket bucket = StorageClient.getInstance().bucket();
        InputStream content = new ByteArrayInputStream(profile.getBytes());
        bucket.create(fileName, content, profile.getContentType());
        return "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/" + fileName + "?alt=media&token=";
    }
}
