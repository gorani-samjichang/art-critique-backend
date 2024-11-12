package com.gorani_samjichang.art_critique.study;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public interface InnerContentsRepository extends JpaRepository<InnerContentsEntity, Long> {
    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsDTO(c.title, c.thumbnailUrl, m.nickname, c.serialNumber, sc.categoryName, sf.categoryTitle, c.createdAt, c.likes, c.view ) from InnerContentsEntity c join c.subCategory sc join sc.field sf join c.author m where c.deletedAt is null order by c.createdAt desc")
    List<InnerContentsDTO> findTop5ByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) from InnerContentsEntity c where c.deletedAt is null order by c.likes desc")
    List<SimpleInnerContentDTO> findTop5ByOrderByLikesDesc(Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO" +
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title, i.view) from InnerContentsEntity i " +
            "join i.author m join i.subCategory sc join sc.field f where sc.categroyNum = :subCategoryNum and i.deletedAt is null and f.categoryNumber = :fieldNum and i.level = :level order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchInnerContentsWithCategoryAndLevel(Long fieldNum, Long subCategoryNum, String level, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO" +
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title, i.view) from InnerContentsEntity i " +
            "join i.author m join i.subCategory sc join sc.field f where sc.categroyNum = :subCategoryNum and f.categoryNumber = :fieldNum and i.deletedAt is null order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchInnerContentsWithCategory(Long fieldNum, Long subCategoryNum, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) " +
            "from InnerContentsEntity c join c.subCategory sc join sc.field f where f.categoryNumber = :fieldNum and c.deletedAt is null  and sc.categroyNum = :subCategoryNum order by c.likes desc")
    List<SimpleInnerContentDTO> findCategoryArticles(Long fieldNum, Long subCategoryNum, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) from InnerContentsEntity c join c.tags tg where tg = :tag and c.deletedAt is null order by c.cid desc")
    List<SimpleInnerContentDTO> searchWithTag(String tag);

    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) " +
            "from InnerContentsEntity c join c.subCategory sc " +
            "where sc.categroyNum = (select c2.subCategory.categroyNum from InnerContentsEntity c2 where c2.serialNumber = :serialNumber) and c.deletedAt is null order by c.likes desc")
    List<SimpleInnerContentDTO> searchWithCategory(String serialNumber, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.ContentsDetailResponseDTO(c.title, m.nickname, m.profile, m.serialNumber, ca.categoryName, f.categoryNumber, ca.categroyNum, c.likes, c.view, c.createdAt, c.deletedAt) " +
            "from InnerContentsEntity c join c.author m join c.subCategory ca join ca.field f where c.serialNumber= :serialNumber")
    Optional<ContentsDetailResponseDTO> searchWithSerialNumberToContentsDetailResponseDTOWithoutDetails(@Param("serialNumber") String serialNumber);

    @Query("select c.tags from InnerContentsEntity c join c.tags where c.serialNumber= :serialNumber")
    List<String> getTags(@Param("serialNumber") String serialNumber);

    @Query("select new com.gorani_samjichang.art_critique.study.ArticleContentsDTO(d.type, d.content) from InnerContentsDetailsEntity d join d.innerContents c where c.serialNumber = :serialNumber order by d.cid asc")
    List<ArticleContentsDTO> findArticleContentBySerialNumber(@Param("serialNumber") String serialNumber);

    @Transactional
    @Modifying
    @Query("UPDATE InnerContentsEntity c SET c.view=c.view+1 where c.serialNumber= :serialNumber")
    int incrementView(@Param("serialNumber") String serialNumber);


    @Transactional
    @Modifying
    @Query("UPDATE InnerContentsEntity c SET c.likes=c.likes+1 where c.serialNumber= :serialNumber")
    int incrementLikes(@Param("serialNumber") String serialNumber);

    @Query("SELECT DISTINCT t FROM InnerContentsEntity i JOIN i.tags t where i.deletedAt is null")
    List<String> findAllTags();


    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO" +
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title, i.view) from InnerContentsEntity i " +
            "join i.author m join i.tags t where t= :tag and i.level = :level and i.deletedAt is null order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchInnerContentsWithTagAndLevel(@Param("tag") String tag, @Param("level") String level, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO" +
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title, i.view) from InnerContentsEntity i " +
            "join i.author m join i.tags t where t= :tag and i.deletedAt is null order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchInnerContentsWithTag(@Param("tag") String tag, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO" +
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title, i.view) from InnerContentsEntity i " +
            "join i.author m where m.serialNumber= :serialNumber and i.deletedAt is null order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchWithMember(@Param("serialNumber") String serialNumber, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.ArticleInfoDTO(count(c), sum(c.view), sum(c.likes)) from InnerContentsEntity c join c.author m where m.serialNumber = :serialNumber and c.deletedAt is null")
    ArticleInfoDTO analyzeInfoOf(@Param("serialNumber") String serialNumber);

    @Query("select distinct t from InnerContentsEntity c join c.author m join c.tags t where m.serialNumber= :serialNumber and c.deletedAt is null")
    List<String> findAllTagsOfMember(@Param("serialNumber")String serialNumber);

    Optional<InnerContentsEntity> findBySerialNumberAndAuthorSerialNumberAndDeletedAtIsNull(@Param("serialNumber") String serialNumber, @Param("authorSerialNumber") String authorSerialNumber);

    @Query("select count(c.cid) from InnerContentsEntity c join c.author m where m.uid=1")
    Long getAdminArticleCount();
}
