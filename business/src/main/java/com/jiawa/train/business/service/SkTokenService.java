package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import com.jiawa.train.business.domain.SkToken;
import com.jiawa.train.business.domain.SkTokenExample;
import com.jiawa.train.business.enums.RedisKeyPreEnum;
import com.jiawa.train.business.mapper.SkTokenMapper;
import com.jiawa.train.business.mapper.customer.SkTokenMapperCust;
import com.jiawa.train.business.req.SkTokenQueryReq;
import com.jiawa.train.business.req.SkTokenSaveReq;
import com.jiawa.train.business.resp.SkTokenQueryResp;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(SkTokenService.class);

    @Autowired
    private SkTokenMapper skTokenMapper;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private DailyTrainStationService dailyTrainStationService;

    @Autowired
    private SkTokenMapperCust skTokenMapperCust;

    @Autowired
    private RedisTemplate redisTemplate;


    @Value("${spring.profiles.active}")
    private String env;

    /**
     * 初始化令牌信息
     */
    @Transactional
    public void genDaily(Date date, String trainCode) {
        LOG.info("删除日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
        //首先删除库里面现有的
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        skTokenMapper.deleteByExample(skTokenExample);

        DateTime now = DateTime.now();
        SkToken skToken = new SkToken();
        skToken.setDate(date);
        skToken.setTrainCode(trainCode);
        skToken.setId(SnowUtil.getSnowflakeNextId());
        skToken.setCreateTime(now);
        skToken.setUpdateTime(now);

        //再通过构造信息重新查询并回填
        int seatCount = dailyTrainSeatService.countSeat(date, trainCode);
        LOG.info("车次【{}】座位数：{}", trainCode, seatCount);

        long stationCount = dailyTrainStationService.countByTrainCode(date, trainCode);
        LOG.info("车次【{}】到站数：{}", trainCode, stationCount);

        // 3/4需要根据实际卖票比例来定(不可能全部卖完)，一趟火车最多可以卖（seatCount * stationCount）张火车票
        int count = (int) (seatCount * stationCount); // * 3/4);
        LOG.info("车次【{}】初始生成令牌数：{}", trainCode, count);
        skToken.setCount(count);

        skTokenMapper.insert(skToken);
    }

    /**
     * 删除2天前的令牌信息
     * @param date
     * @param trainCode
     */
    public void deleteBefore2Days(Date date, String trainCode) {
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.createCriteria().andDateLessThanOrEqualTo(date).andTrainCodeEqualTo(trainCode);
        skTokenMapper.deleteByExample(skTokenExample);
    }

    public void save(SkTokenSaveReq req) {
        DateTime now = DateTime.now();
        SkToken skToken = BeanUtil.copyProperties(req, SkToken.class);
        if (ObjectUtil.isNull(skToken.getId())) {
            skToken.setId(SnowUtil.getSnowflakeNextId());
            skToken.setCreateTime(now);
            skToken.setUpdateTime(now);
            skTokenMapper.insert(skToken);
        } else {
            skToken.setUpdateTime(now);
            skTokenMapper.updateByPrimaryKey(skToken);
        }
    }

    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req) {
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.setOrderByClause("id desc");
        SkTokenExample.Criteria criteria = skTokenExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);

        PageInfo<SkToken> pageInfo = new PageInfo<>(skTokenList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<SkTokenQueryResp> list = BeanUtil.copyToList(skTokenList, SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        skTokenMapper.deleteByPrimaryKey(id);
    }

    /**
     * 校验令牌
     */
    public boolean validSkToken(Date date, String trainCode, Long memberId) {
        LOG.info("会员【{}】获取日期【{}】车次【{}】的令牌开始", memberId, DateUtil.formatDate(date), trainCode);
        String skTokenCountKey = RedisKeyPreEnum.SK_TOKEN_COUNT + "-" + DateUtil.formatDate(date) + "-" + trainCode;
        Object skTokenCount = redisTemplate.opsForValue().get(skTokenCountKey);
        if (skTokenCount != null) {
            LOG.info("缓存中有该车次令牌大闸的key：{}", skTokenCountKey);
            Integer count = (Integer) redisTemplate.opsForValue().get(skTokenCountKey);
            LOG.info("获取令牌前，令牌余数：{}", count);
            redisTemplate.opsForValue().set(skTokenCountKey, count - 1);
            if (count < 0L) {
                LOG.error("获取令牌失败：{}", skTokenCountKey);
                return false;
            } else {
                LOG.info("获取令牌后，令牌余数：{}", count);
                redisTemplate.expire(skTokenCountKey, 60, TimeUnit.SECONDS);
                // 每获取5个令牌更新一次数据库(这个只适合于1分钟内有5次令牌的获取情况，如果1分钟没有5次令牌的获取情况，那么这个方法就失效了)
                if (count % 5 == 0) {
                    skTokenMapperCust.decrease(date, trainCode, 5);
                }
                return true;
            }
        } else {
            LOG.info("缓存中没有该车次令牌大闸的key：{}", skTokenCountKey);
            SkTokenExample skTokenExample = new SkTokenExample();
            skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
            List<SkToken> tokenCountList = skTokenMapper.selectByExample(skTokenExample);
            if (CollUtil.isEmpty(tokenCountList)) {
                LOG.info("找不到日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
                return false;
            }
            SkToken skToken = tokenCountList.get(0);
            if (skToken.getCount() <= 0) {
                LOG.info("日期【{}】车次【{}】的令牌余量为0", DateUtil.formatDate(date), trainCode);
                return false;
            }
            Integer count = skToken.getCount() - 1;
            skToken.setCount(count);
            LOG.info("将该车次令牌大闸放入缓存中，key: {}， count: {}", skTokenCountKey, count);
            redisTemplate.opsForValue().set(skTokenCountKey, count, 60, TimeUnit.SECONDS);
            return true;
        }
    }


//    public boolean validSkToken(Date date, String trainCode, Long memberId) {
//        String dateStr = DateUtil.formatDate(date);
//        LOG.info("会员【{}】获取日期【{}】车次【{}】的令牌开始", memberId, dateStr, trainCode);
//        String skTokenCountKey = RedisKeyPreEnum.SK_TOKEN_COUNT + "-" + dateStr + "-" + trainCode;
//        String skTokenAccKey = RedisKeyPreEnum.SK_TOKEN_COUNT + "-acc-" + dateStr + "-" + trainCode;
//
//        try {
//            // ========== 第一步：原子检查并扣减令牌（核心修复：Lua脚本） ==========
//            // Lua脚本：先检查key的值>0，再扣减1并返回扣减后的值；否则返回-1（原子操作）
//            String luaScript = "local count = redis.call('get', KEYS[1]) " +
//                    "if count == false then return -2 end " +  // key不存在返回-2
//                    "count = tonumber(count) " +
//                    "if count <= 0 then return -1 end " +     // 令牌已耗尽返回-1
//                    "redis.call('decr', KEYS[1]) " +
//                    "return count - 1";                      // 扣减后的值
//            RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
//            Long result = (Long) redisTemplate.execute(redisScript, Arrays.asList(skTokenCountKey));
//
//            // ========== 分支1：缓存存在且令牌充足（扣减成功） ==========
//            if (result != null && result >= 0) {
//                LOG.info("缓存中有该车次令牌，key：{}，扣减前令牌数：{}，扣减后：{}",
//                        skTokenCountKey, result + 1, result);
//
//                // 累计扣减次数，达到5次同步数据库
//                Long accCount = redisTemplate.opsForValue().increment(skTokenAccKey, 1);
//                if (accCount == 1) {
//                    redisTemplate.expire(skTokenAccKey, 60, TimeUnit.SECONDS);
//                }
//                if (accCount % 5 == 0) {
//                    LOG.info("累计扣减{}次令牌，同步数据库，车次：{}，日期：{}", accCount, trainCode, dateStr);
//                    decreaseSkToken(date, trainCode, 5);
//                    redisTemplate.delete(skTokenAccKey);
//                }
//
//                // 确保缓存过期时间有效
//                if (redisTemplate.getExpire(skTokenCountKey) < 0) {
//                    redisTemplate.expire(skTokenCountKey, 60, TimeUnit.SECONDS);
//                }
//                return true;
//            }
//
//            // ========== 分支2：缓存存在但令牌已耗尽（值为0/负数） ==========
//            if (result != null && result == -1) {
//                LOG.error("获取令牌失败：{}，令牌已耗尽（缓存值为0）", skTokenCountKey);
//                // 主动删除无效缓存，下次请求重新从数据库加载
//                redisTemplate.delete(skTokenCountKey);
//                return false;
//            }
//
//            // ========== 分支3：缓存不存在（result=-2），从数据库加载并初始化 ==========
//            LOG.info("缓存中无该车次令牌，key：{}，从数据库加载", skTokenCountKey);
//            SkTokenExample skTokenExample = new SkTokenExample();
//            skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
//            List<SkToken> tokenCountList = skTokenMapper.selectByExample(skTokenExample);
//
//            if (CollectionUtils.isEmpty(tokenCountList)) {
//                LOG.info("数据库无日期【{}】车次【{}】的令牌记录", dateStr, trainCode);
//                return false;
//            }
//
//            SkToken skToken = tokenCountList.get(0);
//            Integer dbCount = skToken.getCount();
//            if (dbCount <= 0) {
//                LOG.info("数据库中日期【{}】车次【{}】的令牌余量为0", dateStr, trainCode);
//                return false;
//            }
//
//            // 原子初始化缓存：扣减1个后存入（避免并发重复初始化）
//            Integer initCount = dbCount - 1;
//            Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(skTokenCountKey, initCount.intValue(), 60, TimeUnit.SECONDS);
//
//            // 并发场景下其他线程已初始化缓存：直接返回成功（因为数据库已扣减1个，缓存也已初始化）
//            if (Boolean.FALSE.equals(setSuccess)) {
//                LOG.info("并发初始化缓存，其他线程已完成，车次：{}，日期：{}", trainCode, dateStr);
//                // 补充累计计数器（避免漏统计）
//                redisTemplate.opsForValue().increment(skTokenAccKey, 1);
//                return true;
//            }
//
//            LOG.info("初始化令牌缓存，key：{}，初始令牌数：{}，扣减后：{}",
//                    skTokenCountKey, dbCount, initCount);
//            // 初始化累计计数器
//            redisTemplate.opsForValue().setIfAbsent(skTokenAccKey, 1, 60, TimeUnit.SECONDS);
//            return true;
//
//        } catch (Exception e) {
//            LOG.error("会员【{}】令牌校验异常，日期：{}，车次：{}", memberId, dateStr, trainCode, e);
//            return false;
//        }
//    }
//
//    /**
//     * 数据库扣减令牌（事务保障）
//     */
//    @Transactional(rollbackFor = Exception.class)
//    protected void decreaseSkToken(Date date, String trainCode, int deductNum) {
//        skTokenMapperCust.decrease(date, trainCode, deductNum);
//        // 校验扣减后令牌数不能为负，否则回滚
//        SkTokenExample example = new SkTokenExample();
//        example.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
//        List<SkToken> list = skTokenMapper.selectByExample(example);
//        if (!CollectionUtils.isEmpty(list) && list.get(0).getCount() < 0) {
//            throw new RuntimeException("令牌扣减后为负，车次：" + trainCode + "，日期：" + DateUtil.formatDate(date));
//        }
//    }


}
