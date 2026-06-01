package com.tourwise.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminImageMapper {
    int existsPoi(@Param("poiId") Long poiId);

    int existsSpot(@Param("spotId") Long spotId);

    int existsFood(@Param("foodId") Long foodId);

    int existsCircle(@Param("circleId") Long circleId);

    int updatePoiImage(@Param("poiId") Long poiId, @Param("imageUrl") String imageUrl);

    int updateSpotCover(@Param("spotId") Long spotId, @Param("imageUrl") String imageUrl);

    int updateSpotCoverByPoiId(@Param("poiId") Long poiId, @Param("imageUrl") String imageUrl);

    int updateRepresentativePoiImage(@Param("spotId") Long spotId, @Param("imageUrl") String imageUrl);

    int updateFoodImage(@Param("foodId") Long foodId, @Param("imageUrl") String imageUrl);

    int updateCircleCover(@Param("circleId") Long circleId, @Param("imageUrl") String imageUrl);
}
