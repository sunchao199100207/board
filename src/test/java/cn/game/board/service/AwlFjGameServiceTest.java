package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 阿瓦隆反法西斯游戏服务单元测试类
 * 
 * <p>功能说明：
 * <ul>
 *   <li>测试任意人数局（5-12人）的发牌逻辑</li>
 *   <li>统计所有座位的角色分布概率</li>
 *   <li>验证随机分配算法的公平性</li>
 * </ul>
 * 
 * <p>使用方法：
 * 修改 {@link #PLAYER_COUNT} 常量可测试不同人数局
 * 修改 {@link #TOTAL_RUNS} 常量可调整模拟次数（建议10000次以上）
 * 
 * @author rider
 * @since 2024-11-19
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AwlFjGameServiceTest {

    /**
     * 模拟的消息发送模板（Mock对象）
     */
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 被测试的游戏服务实例（注入Mock依赖）
     */
    @InjectMocks
    private AwlFjGameService awlFjGameService;

    /**
     * 通过反射获取的私有方法 checkGameStart
     */
    private Method checkGameStartMethod;
    
    /**
     * 测试用的游戏房间实例
     */
    private GameRoom testRoom;

    /**
     * 测试人数（可修改为 5-12 之间的任意值）
     */
    private static final int PLAYER_COUNT = 9;
    
    /**
     * 模拟运行总次数（建议10000次以上以获得稳定的概率分布）
     */
    private static final int TOTAL_RUNS = 10000;
    
    /**
     * 支持的最小人数
     */
    private static final int MIN_PLAYER_COUNT = 5;
    
    /**
     * 支持的最大人数
     */
    private static final int MAX_PLAYER_COUNT = 12;

    /**
     * 统计所有座位的角色分布计数器
     * <p>数据结构：
     * <ul>
     *   <li>外层 Key: 座位号（Integer，范围 1-PLAYER_COUNT）</li>
     *   <li>内层 Key: 角色名称（String）</li>
     *   <li>内层 Value: 该角色在该座位出现的次数（Integer）</li>
     * </ul>
     */
    private Map<Integer, Map<String, Integer>> allSeatsRoleCounts;

    /**
     * 根据人数动态确定的所有可能角色列表（去重并排序后）
     */
    private List<String> roleOrder;

    /**
     * 不同人数对应的角色配置映射表
     * <p>注意：该配置必须与 {@link AwlFjGameService} 中的 IDENTITIES_MAP 保持一致
     * 
     * <p>角色说明：
     * <ul>
     *   <li>好人阵营：梅林、派西维尔、亚瑟的忠臣、兰斯洛特(蓝)</li>
     *   <li>坏人阵营：莫甘娜、刺客、莫德雷德、奥伯伦、莫德雷德的爪牙、兰斯洛特(红)</li>
     * </ul>
     */
    private static final Map<Integer, List<String>> IDENTITIES_MAP = Map.ofEntries(
            Map.entry(5, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客")),
            Map.entry(6, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣")),
            Map.entry(7, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "奥伯伦")),
            Map.entry(8, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", 
                    "莫德雷德的爪牙")),
            Map.entry(9, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", 
                    "亚瑟的忠臣", "莫德雷德")),
            Map.entry(10, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", 
                    "亚瑟的忠臣", "莫德雷德", "奥伯伦")),
            Map.entry(11, List.of("梅林", "派西维尔", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", 
                    "兰斯洛特(蓝)", "莫德雷德", "莫甘娜", "刺客", "兰斯洛特(红)")),
            Map.entry(12, List.of("梅林", "派西维尔", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", 
                    "兰斯洛特(蓝)", "莫德雷德", "莫甘娜", "刺客", "奥伯伦", "兰斯洛特(红)")));

    /**
     * 全局测试初始化方法
     * <p>在所有测试用例执行前运行一次，负责：
     * <ol>
     *   <li>验证测试人数配置的有效性</li>
     *   <li>通过反射获取被测试的私有方法</li>
     *   <li>初始化测试房间和玩家</li>
     *   <li>初始化角色统计数据结构</li>
     * </ol>
     * 
     * @throws Exception 如果反射获取方法失败或人数配置无效
     */
    @BeforeAll
    void globalSetUp() throws Exception {
        printTestHeader();
        
        // 1. 验证人数配置是否有效
        validatePlayerCount();

        // 2. 获取私有方法（通过反射）
        initCheckGameStartMethod();

        // 3. 初始化测试房间和玩家
        initTestRoom();

        // 4. 初始化角色列表（去重并排序）
        initRoleOrder();

        // 5. 初始化所有座位的角色计数器
        initAllSeatsRoleCounts();

        printInitializationSummary();
    }

    /**
     * 打印测试开始的头部信息
     */
    private void printTestHeader() {
        System.out.println("========================================");
        System.out.println("--- 全局测试设置 ---");
        System.out.println("测试人数: " + PLAYER_COUNT + " 人局");
        System.out.println("测试次数: " + TOTAL_RUNS + " 次");
        System.out.println("========================================");
    }

    /**
     * 验证测试人数配置的有效性
     * 
     * @throws IllegalArgumentException 如果人数不在支持范围内（5-12人）
     */
    private void validatePlayerCount() {
        if (!IDENTITIES_MAP.containsKey(PLAYER_COUNT)) {
            String errorMsg = String.format("不支持的人数配置: %d。支持的人数: %d-%d", 
                    PLAYER_COUNT, MIN_PLAYER_COUNT, MAX_PLAYER_COUNT);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * 通过反射获取 AwlFjGameService 的私有方法 checkGameStart
     * 
     * @throws NoSuchMethodException 如果方法不存在
     */
    private void initCheckGameStartMethod() throws NoSuchMethodException {
        checkGameStartMethod = AwlFjGameService.class.getDeclaredMethod("checkGameStart", GameRoom.class);
        checkGameStartMethod.setAccessible(true);
    }

    /**
     * 初始化测试房间和玩家
     * <p>创建指定人数的玩家，所有玩家默认处于准备状态
     * <p>注意：房间号必须是纯数字字符串，因为 checkGameStart 方法会用它生成随机种子
     */
    private void initTestRoom() {
        // 使用纯数字房间号（AwlFjGameService 的 checkGameStart 方法会将其解析为 Long）
        testRoom = new GameRoom("123456", PLAYER_COUNT, "hostSessionId");
        testRoom.setGameCount(0);
        
        // 创建玩家并加入房间
        for (int seatNumber = 1; seatNumber <= PLAYER_COUNT; seatNumber++) {
            Player player = new Player("session_seat_" + seatNumber);
            player.setSeatNumber(seatNumber);
            player.setReady(true);
            testRoom.getPlayers().put(player.getSessionId(), player);
        }
    }

    /**
     * 初始化角色顺序列表
     * <p>从配置中获取当前人数对应的角色列表，去重并按字母顺序排序
     */
    private void initRoleOrder() {
        List<String> identities = IDENTITIES_MAP.get(PLAYER_COUNT);
        roleOrder = identities.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 初始化所有座位的角色统计计数器
     * <p>为每个座位创建一个角色计数Map，初始值都为0
     */
    private void initAllSeatsRoleCounts() {
        allSeatsRoleCounts = new HashMap<>(PLAYER_COUNT);
        
        for (int seatNumber = 1; seatNumber <= PLAYER_COUNT; seatNumber++) {
            Map<String, Integer> roleCountMap = new HashMap<>(roleOrder.size());
            for (String role : roleOrder) {
                roleCountMap.put(role, 0);
            }
            allSeatsRoleCounts.put(seatNumber, roleCountMap);
        }
    }

    /**
     * 打印初始化完成的摘要信息
     */
    private void printInitializationSummary() {
        List<String> identities = IDENTITIES_MAP.get(PLAYER_COUNT);
        System.out.println("房间和 " + PLAYER_COUNT + " 个座位的计数器初始化完毕。");
        System.out.println("本局角色配置: " + identities);
        System.out.println("所有可能角色: " + roleOrder);
        System.out.println("--- 开始 " + TOTAL_RUNS + " 次模拟发牌 ---\n");
    }

    /**
     * 重复测试：模拟连续多局游戏的发牌逻辑
     * <p>该测试方法会重复执行 {@link #TOTAL_RUNS} 次，每次模拟一局游戏的发牌过程
     * 
     * <p>测试步骤：
     * <ol>
     *   <li>重置游戏状态</li>
     *   <li>调用游戏开始方法（通过反射）</li>
     *   <li>验证游戏局数正确递增</li>
     *   <li>统计每个座位的角色分配情况</li>
     *   <li>打印本局发牌结果</li>
     * </ol>
     * 
     * @param repetitionInfo JUnit提供的重复测试信息对象
     * @throws Exception 如果反射调用方法失败
     */
    @RepeatedTest(value = TOTAL_RUNS, name = "模拟发牌 第 {currentRepetition} / {totalRepetitions} 局")
    @DisplayName("模拟发牌并统计所有座位的角色分布")
    void testRoleDistributionForAllSeats(RepetitionInfo repetitionInfo) throws Exception {

        // 1. 准备阶段：重置游戏开始状态
        testRoom.setGameStarted(false);

        // 2. 执行阶段：通过反射调用 checkGameStart 方法
        checkGameStartMethod.invoke(awlFjGameService, testRoom);

        // 3. 验证阶段：检查游戏局数是否正确递增
        assertEquals(repetitionInfo.getCurrentRepetition(), testRoom.getGameCount(), 
                "游戏局数应该递增");

        // 4. 统计阶段：收集并统计本局发牌结果
        List<Player> sortedPlayers = getSortedPlayers();
        updateRoleStatistics(sortedPlayers);
        
        // 5. 输出阶段：打印本局发牌详情
        printCurrentRoundResult(repetitionInfo.getCurrentRepetition(), sortedPlayers);
    }

    /**
     * 获取按座位号排序的玩家列表
     * 
     * @return 排序后的玩家列表
     */
    private List<Player> getSortedPlayers() {
        List<Player> players = new ArrayList<>(testRoom.getPlayers().values());
        players.sort(Comparator.comparingInt(Player::getSeatNumber));
        return players;
    }

    /**
     * 更新角色统计数据
     * <p>遍历所有玩家，将其角色分配情况记录到统计计数器中
     * 
     * @param players 玩家列表
     */
    private void updateRoleStatistics(List<Player> players) {
        for (Player player : players) {
            int seatNumber = player.getSeatNumber();
            String identity = player.getIdentity();

            // 断言：角色不应为空
            assertNotNull(identity, "座位 " + seatNumber + " 的身份不应为 null");

            // 更新该座位的角色计数
            Map<String, Integer> seatCounts = allSeatsRoleCounts.get(seatNumber);
            seatCounts.put(identity, seatCounts.get(identity) + 1);
        }
    }

    /**
     * 打印当前局次的发牌结果
     * 
     * @param currentRound 当前局数
     * @param players 玩家列表
     */
    private void printCurrentRoundResult(int currentRound, List<Player> players) {
        StringBuilder output = new StringBuilder();
        output.append("第 ").append(String.format("%-5d", currentRound)).append(" 局: [");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (Player player : players) {
            joiner.add(String.format("%d号:%s", player.getSeatNumber(), player.getIdentity()));
        }
        
        output.append(joiner);
        output.append("] - 领袖: ").append(testRoom.getCurrentLeaderSeat()).append(" 号");
        System.out.println(output);
    }

    /**
     * 测试结束后的汇总方法
     * <p>在所有测试用例执行完毕后运行一次，负责：
     * <ol>
     *   <li>打印测试汇总信息</li>
     *   <li>打印角色配置和预期概率</li>
     *   <li>打印每个座位的角色分布统计</li>
     *   <li>验证统计数据的正确性</li>
     * </ol>
     */
    @AfterAll
    void printAllSeatsSummary() {
        printSummaryHeader();
        
        // 计算角色频率（每个角色在配置中出现的次数）
        Map<String, Long> roleFrequency = calculateRoleFrequency();
        
        // 打印角色配置信息
        printRoleConfiguration(roleFrequency);
        
        // 打印每个座位的统计数据
        printEachSeatStatistics(roleFrequency);
        
        printSummaryFooter();
    }

    /**
     * 打印汇总报告的头部信息
     */
    private void printSummaryHeader() {
        System.out.println("\n==============================================");
        System.out.println("--- 模拟 " + TOTAL_RUNS + " 局发牌结束 ---");
        System.out.println("【所有座位】角色分布统计 (" + PLAYER_COUNT + " 人局):");
        System.out.println("==============================================");
    }

    /**
     * 计算每个角色在配置中的出现频率
     * <p>统计当前人数配置下，每个角色出现的次数
     * 
     * @return 角色频率映射表（Key: 角色名, Value: 出现次数）
     */
    private Map<String, Long> calculateRoleFrequency() {
        List<String> identities = IDENTITIES_MAP.get(PLAYER_COUNT);
        Map<String, Long> roleFrequency = new HashMap<>(identities.size());
        
        for (String role : identities) {
            roleFrequency.put(role, roleFrequency.getOrDefault(role, 0L) + 1);
        }
        
        return roleFrequency;
    }

    /**
     * 打印角色配置信息
     * <p>展示当前人数下的角色配置和每个角色的理论概率
     * 
     * @param roleFrequency 角色频率映射表
     */
    private void printRoleConfiguration(Map<String, Long> roleFrequency) {
        System.out.println("\n【角色配置】");
        
        for (String role : roleOrder) {
            long count = roleFrequency.getOrDefault(role, 0L);
            double expectedPercentage = (double) count / PLAYER_COUNT * 100;
            System.out.printf("  - %-15s: %d 个 (预期概率: %.1f%%)%n", 
                    role, count, expectedPercentage);
        }
    }

    /**
     * 打印每个座位的角色分布统计
     * <p>遍历所有座位，打印每个座位上各角色的实际分布情况，并与理论值对比
     * 
     * @param roleFrequency 角色频率映射表
     */
    private void printEachSeatStatistics(Map<String, Long> roleFrequency) {
        for (int seatNumber = 1; seatNumber <= PLAYER_COUNT; seatNumber++) {
            printSingleSeatStatistics(seatNumber, roleFrequency);
        }
    }

    /**
     * 打印单个座位的角色分布统计
     * 
     * @param seatNumber 座位号
     * @param roleFrequency 角色频率映射表
     */
    private void printSingleSeatStatistics(int seatNumber, Map<String, Long> roleFrequency) {
        System.out.printf("%n--- 【%d 号座位】统计 ---%n", seatNumber);
        
        Map<String, Integer> seatCounts = allSeatsRoleCounts.get(seatNumber);
        long totalCount = 0;

        // 打印该座位上每个角色的统计数据
        for (String role : roleOrder) {
            int actualCount = seatCounts.getOrDefault(role, 0);
            double actualPercentage = (double) actualCount / TOTAL_RUNS * 100;
            
            long expectedCount = roleFrequency.getOrDefault(role, 0L);
            double expectedPercentage = (double) expectedCount / PLAYER_COUNT * 100;

            System.out.printf("  - %-15s: %5d 次 (%5.1f%%) [预期: %5.1f%%]%n",
                    role, actualCount, actualPercentage, expectedPercentage);
            
            totalCount += actualCount;
        }

        // 打印该座位的总计
        System.out.println("  ---------------------------");
        System.out.printf("  总计: %d 次 (100.0%%)%n", totalCount);
        
        // 验证总次数是否正确
        assertEquals(TOTAL_RUNS, totalCount, 
                "座位 " + seatNumber + " 的总次数应为 " + TOTAL_RUNS);
    }

    /**
     * 打印汇总报告的尾部说明信息
     */
    private void printSummaryFooter() {
        System.out.println("\n==============================================");
        System.out.println("说明:");
        System.out.println("  - 由于随机性，每个座位的分布都会略有不同");
        System.out.println("  - 但理论上都应趋近于预期概率");
        System.out.println("  - 运行次数越多，分布越接近理论值");
        System.out.println("  - 如果某个座位的某个角色偏差过大(>5%)，可能需要检查随机算法");
        System.out.println("==============================================");
    }
}