package top.zzf4.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.zzf4.blog.entity.model.DailyStats;

@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {
}
