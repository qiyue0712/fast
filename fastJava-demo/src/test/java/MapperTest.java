import com.wjy.RunDemoApplication;
import com.wjy.entity.po.UserInfo;
import com.wjy.entity.query.UserInfoQuery;
import com.wjy.mappers.UserInfoMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest(classes = RunDemoApplication.class)
@ExtendWith(SpringExtension.class)
public class MapperTest {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Test
    public void selectList() {
        UserInfoQuery query = new UserInfoQuery();

        // 使用时间范围查询
        query.setJoinTimeStart("2025-01-01");
        query.setJoinTimeEnd("2025-12-31");

        List<UserInfo> dataList = userInfoMapper.selectList(query);
        for (UserInfo userInfo : dataList) {
            System.out.println(userInfo);
        }

        Long count = userInfoMapper.selectCount(query);
        System.out.println(count);
    }

    @Test
    public void insert() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("19");
        userInfo.setEmail("16432ewe1312e3efe");
        userInfo.setNickName("te2331fe32ffef");
        this.userInfoMapper.insert(userInfo);
        System.out.println(userInfo.getUserId());
        System.out.println(userInfo);
    }

    @Test
    public void insertOrUpdate() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("9");
        userInfo.setEmail("test12sqfe32376321sqs");
        userInfo.setNickName("te2331fe32f");
        this.userInfoMapper.insertOrUpdate(userInfo);
        System.out.println(userInfo.getUserId());
        System.out.println(userInfo);
    }

    @Test
    public void insertBatch() {
        List<UserInfo> userInfoList = new ArrayList<>();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("20");
        userInfo.setEmail("13093213hghe3efffe");
        userInfo.setNickName("toj23fffeef");
        userInfo.setPassword("123456");
        userInfo.setJoinTime(new Date());
        userInfoList.add(userInfo);

        userInfo = new UserInfo();
        userInfo.setUserId("21");
        userInfo.setEmail("1354ojg54efffe");
        userInfo.setNickName("tegf123pgfeef");
        userInfoList.add(userInfo);

        userInfoMapper.insertBatch(userInfoList);
        System.out.println(userInfoList);
    }

    @Test
    public void insertBatchOrUpdate() {
        List<UserInfo> userInfoList = new ArrayList<>();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("18");
        userInfo.setNickName("tegj23fffe");
        userInfo.setEmail("135443213hghe3efffe");
        userInfoList.add(userInfo);

        userInfoMapper.insertOrUpdateBatch(userInfoList);
        System.out.println(userInfoList);
    }
}