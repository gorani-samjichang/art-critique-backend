package com.gorani_samjichang.art_critique.study;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface InnerContentsRepository extends JpaRepository<InnerContentsEntity, Long> {
    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsDTO(c.title, c.thumbnailUrl, m.nickname, c.serialNumber, sc.categoryName, sf.categoryTitle, c.createdAt, c.likes, c.view ) from InnerContentsEntity c join c.subCategory sc join sc.field sf join c.author m order by c.createdAt desc")
    List<InnerContentsDTO> findTop5ByOrderByCreatedAtDesc(Pageable pageable);
    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) from InnerContentsEntity c order by c.likes desc")
    List<SimpleInnerContentDTO> findTop5ByOrderByLikesDesc(Pageable pageable);
    Optional<InnerContentsEntity> findBySerialNumber(String serialNumber);
    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO"+
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title) from InnerContentsEntity i " +
            "join i.author m join i.subCategory sc join sc.field f where sc.categroyNum = :subCategoryNum and f.categoryNumber = :fieldNum and i.level = :level order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchInnerContentsWithCategoryAndLevel(Long fieldNum, Long subCategoryNum, String level, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.InnerContentsCategoryDTO"+
            "(m.nickname, m.profile, i.createdAt, i.serialNumber, i.thumbnailUrl, " +
            "i.likes,CASE WHEN i.level = 'newbie' THEN '입문' WHEN i.level = 'chobo' THEN '초보' WHEN i.level = 'intermediate' THEN '중수' WHEN i.level = 'gosu' THEN '고수' ELSE '미분류' END, i.title) from InnerContentsEntity i " +
            "join i.author m join i.subCategory sc join sc.field f where sc.categroyNum = :subCategoryNum and f.categoryNumber = :fieldNum order by i.createdAt desc")
    List<InnerContentsCategoryDTO> searchInnerContentsWithCategory(Long fieldNum, Long subCategoryNum, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) " +
            "from InnerContentsEntity c join c.subCategory sc join sc.field f where f.categoryNumber = :fieldNum and sc.categroyNum = :subCategoryNum order by c.likes desc")
    List<SimpleInnerContentDTO> findCategoryArticles(Long fieldNum, Long subCategoryNum, Pageable pageable);

    @Query("select new com.gorani_samjichang.art_critique.study.SimpleInnerContentDTO(c.title, c.serialNumber) from InnerContentsEntity c join c.tags tg where tg = :tag order by c.cid desc")
    List<SimpleInnerContentDTO> searchWithTag(String tag);

    @Transactional
    @Modifying
    @Query("UPDATE InnerContentsEntity c SET c.view=c.view+1 where c.serialNumber= :serialNumber")
    int incrementView(@Param("serialNumber") String serialNumber);


    @Transactional
    @Modifying
    @Query("UPDATE InnerContentsEntity c SET c.likes=c.likes+1 where c.serialNumber= :serialNumber")
    int incrementLikes(@Param("serialNumber") String serialNumber);
}
