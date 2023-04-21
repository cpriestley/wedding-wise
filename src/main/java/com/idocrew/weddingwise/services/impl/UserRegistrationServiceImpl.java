package com.idocrew.weddingwise.services.impl;

import com.idocrew.weddingwise.context.AccountVerificationEmailContext;
import com.idocrew.weddingwise.entity.*;
import com.idocrew.weddingwise.repositories.*;
import com.idocrew.weddingwise.services.EmailService;
import com.idocrew.weddingwise.services.SecureTokenService;
import com.idocrew.weddingwise.services.UserRegistrationService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service("userService")
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final CustomerRepository customerRepository;
    private final PrincipalGroupRepository principalGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VenueRepository venueRepository;
    private final DjsAndLiveBandsRepository djsAndLiveBandsRepository;
    private final DjsAndLiveBandsMusicGenreRepository djsAndLiveBandsMusicGenreRepository;
    private final VendorsPhotoFormatRepository vendorsPhotoFormatRepository;
    private final SecureTokenService secureTokenService;
    @Value("${site.base.url.https}")
    private String baseURL;

    @Override
    @Transactional
    public void register(Customer customer) throws DuplicateKeyException {
        if(checkIfUserExist(customer.getUser().getEmail())){
            throw new DuplicateKeyException("Customer already exists for this email");
        }
        User userEntity = saveUser(customer.getUser(), "CUSTOMER");
        customer.setUser(userEntity);
        saveCustomer(customer);
        sendRegistrationConfirmationEmail(userEntity);
    }

    private void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void register(VendorComposite vendorComposite) {
        if(checkIfUserExist(vendorComposite.getUser().getEmail())){
            throw new DuplicateKeyException("Vendor already exists for this email");
        }

        User userEntity = saveUser(vendorComposite.getUser(), "VENDOR");
        vendorComposite.getVendor().setUser(userEntity);
        Vendor vendorEntity = saveVendor(vendorComposite.getVendor());

        switch (vendorEntity.getVendorCategory().getTitle()) {
            case "Venues" -> saveVenue(vendorEntity, vendorComposite);
            case "Photographers" -> savePhotographer(vendorEntity, vendorComposite.getPhotoFormat());
            case "Bands and DJs" -> {
                DjsAndLiveBand djOrLiveBand = saveDjOrBand(vendorEntity, vendorComposite.getDjsAndLiveBandsCategory());
                saveDjOrBandMusicGenres(djOrLiveBand, vendorComposite.getMusicGenres());
            }
            default -> {
            }
        }
        sendRegistrationConfirmationEmail(userEntity);
    }

    private DjsAndLiveBand saveDjOrBand(Vendor vendorEntity, DjsAndLiveBandsCategory category) {
        DjsAndLiveBand djOrLiveBand = new DjsAndLiveBand(vendorEntity, category);
        return djsAndLiveBandsRepository.save(djOrLiveBand);
    }

    private void saveDjOrBandMusicGenres(DjsAndLiveBand djsOrLiveBand, Set<MusicGenre> musicGenres) {
        Set<DjsAndLiveBandsMusicGenre> set = musicGenres
                .stream()
                .map(musicGenre -> new DjsAndLiveBandsMusicGenre(djsOrLiveBand, musicGenre))
                .collect(Collectors.toSet());
        djsAndLiveBandsMusicGenreRepository.saveAll(set);
    }

    private void savePhotographer(Vendor vendorEntity, PhotoFormat photoFormat) {
        VendorsPhotoFormat vendorsPhotoFormatEntity = new VendorsPhotoFormat();
        vendorsPhotoFormatEntity.setVendor(vendorEntity);
        vendorsPhotoFormatEntity.setPhotoFormat(photoFormat);
        vendorsPhotoFormatRepository.save(vendorsPhotoFormatEntity);
    }

    private void saveVenue(Vendor vendorEntity, VendorComposite vendorComposite) {
        Venue venueEntity = new Venue();
        BeanUtils.copyProperties(vendorComposite.getVenue(), venueEntity);
        venueEntity.setVendor(vendorEntity);
        venueRepository.save(venueEntity);
    }

    private Vendor saveVendor(Vendor vendor) {
        Vendor vendorEntity = new Vendor();
        BeanUtils.copyProperties(vendor, vendorEntity);
        return vendorRepository.save(vendorEntity);
    }

    private User saveUser(User user, String code) {
        User userEntity = new User();
        BeanUtils.copyProperties(user, userEntity);
        String hash = passwordEncoder.encode(user.getPassword());
        userEntity.setPassword(hash);
        userEntity.setUsername(user.getEmail());
        userEntity.setUserGroups(getUserGroupAsSet(code));
        userEntity = userRepository.save(userEntity);
        return userEntity;
    }

    private boolean checkIfUserExist(String email) {
        return userRepository.findByEmail(email) != null;
    }

    private void sendRegistrationConfirmationEmail(User userEntity) {
        SecureToken secureToken = secureTokenService.createSecureToken(userEntity);

        AccountVerificationEmailContext emailContext = new AccountVerificationEmailContext();
        emailContext.init(userEntity);
        emailContext.setToken(secureToken.getToken());
        emailContext.buildVerificationUrl(baseURL, secureToken.getToken());
        try {
            emailService.sendVerificationEmail(emailContext);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private Set<PrincipalGroup> getUserGroupAsSet(String role){
        return Set.of(principalGroupRepository.findByCode(role));
    }
}