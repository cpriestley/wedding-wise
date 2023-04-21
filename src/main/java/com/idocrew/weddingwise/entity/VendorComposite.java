package com.idocrew.weddingwise.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class VendorComposite {
    private Vendor vendor;
    private Venue venue;
    private Set<MusicGenre> musicGenres;
    private PhotoFormat photoFormat;
    private DjsAndLiveBandsCategory djsAndLiveBandsCategory;
}
