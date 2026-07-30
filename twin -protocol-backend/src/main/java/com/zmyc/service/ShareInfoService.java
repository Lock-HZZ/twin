package com.zmyc.service;

import com.zmyc.application.vo.response.PageResponse;
import com.zmyc.application.vo.response.ShareInfoResponse;
import com.zmyc.application.vo.response.ShareListItemResponse;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.entity.UserPerformanceDO;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.infrastructure.repository.UserPerformanceRepository;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareInfoService {

    private final UserRepository userRepository;
    private final UserDepositRepository userDepositRepository;
    private final UserRelationClosureRepository closureRepository;
    private final UserPerformanceRepository performanceRepository;
    private final RewardService rewardService;

    public ShareInfoResponse getShareInfo(Long userId) {
        UserDO user = userRepository.findById(userId);
        UserPerformanceDO perf = performanceRepository.findByUserId(userId);

        BigDecimal communityVolume = perf != null && perf.getCommunityVolumeUsdt() != null
                ? perf.getCommunityVolumeUsdt() : BigDecimal.ZERO;
        BigDecimal teamDepositAmount = userDepositRepository.sumTeamDepositAmount(userId);
        BigDecimal directDepositAmount = closureRepository.sumDirectDepositAmount(userId);

        return ShareInfoResponse.builder()
                .address(user.getAddress())
                .role(user.getRole())
                .roleName(getRoleName(user.getRole()))
                .dLevel(user.getLevel())
                .sLevel(rewardService.getSLevelName(communityVolume))
                .isValid(userDepositRepository.isValidUser(userId))
                .totalDirectCount(closureRepository.countAllDirectChildren(userId))
                .validDirectCount((long) closureRepository.countValidDirectChildren(userId))
                .totalTeamCount(closureRepository.countDescendants(userId))
                .validTeamCount(closureRepository.countValidTeamMembers(userId))
                .directDepositAmount(directDepositAmount != null ? directDepositAmount : BigDecimal.ZERO)
                .teamDepositAmount(teamDepositAmount != null ? teamDepositAmount : BigDecimal.ZERO)
                .communityDepositAmount(communityVolume)
                .build();
    }

    public PageResponse<ShareListItemResponse> getShareList(Long userId, Integer page, Integer pageSize) {
        long total = closureRepository.countAllDirectChildren(userId);
        int offset = (page - 1) * pageSize;

        List<Long> childIds = closureRepository.findDirectChildrenIds(userId, offset, pageSize);

        List<ShareListItemResponse> list = childIds.stream().map(childId -> {
            UserDO child = userRepository.findById(childId);
            UserPerformanceDO perf = performanceRepository.findByUserId(childId);

            BigDecimal communityVol = perf != null && perf.getCommunityVolumeUsdt() != null
                    ? perf.getCommunityVolumeUsdt() : BigDecimal.ZERO;
            BigDecimal teamVol = perf != null && perf.getTeamVolumeUsdt() != null
                    ? perf.getTeamVolumeUsdt() : BigDecimal.ZERO;

            return ShareListItemResponse.builder()
                    .address(child.getAddress())
                    .createdDate(child.getCreatedDate())
                    .isValid(userDepositRepository.isValidUser(childId))
                    .role(child.getRole())
                    .roleName(getRoleName(child.getRole()))
                    .dLevel(child.getLevel())
                    .sLevel(rewardService.getSLevelName(communityVol))
                    .totalTeamCount(closureRepository.countDescendants(childId))
                    .validTeamCount(closureRepository.countValidTeamMembers(childId))
                    .teamDepositAmount(teamVol)
                    .build();
        }).collect(Collectors.toList());

        return new PageResponse<>(list, total, page, pageSize);
    }

    private String getRoleName(Integer role) {
        if (role == null) return "普通用户";
        return switch (role) {
            case UserDO.Role.GOLD    -> "黄金节点";
            case UserDO.Role.DIAMOND -> "钻石节点";
            case UserDO.Role.CROWN   -> "皇冠节点";
            case UserDO.Role.PARTNER -> "合伙人";
            default                  -> "普通用户";
        };
    }
}
