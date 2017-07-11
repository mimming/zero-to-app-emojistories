//  Copyright 2017 Google
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

//
//  ViewController.swift
//  Emojistories
//

import UIKit

import Firebase
import FirebaseAuthUI
import FirebaseGoogleAuthUI

class FirestoriesViewController: UIViewController, UICollectionViewDelegate, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout, UIImagePickerControllerDelegate, UINavigationControllerDelegate, FUIAuthDelegate {
    
    @IBOutlet weak var collectionView: UICollectionView!
    @IBOutlet weak var textInput: UITextField!
    @IBOutlet weak var takePhotoButton: UIButton!
    
    let reuseIdentifier = "StoryCell"
    let sectionInsets = UIEdgeInsets(top: 50.0, left: 20.0, bottom: 50.0, right: 20.0)
    let itemsPerRow: CGFloat = 3
    
    var storiesArray: [[String: Any]]! = []
    
    let imagePicker = UIImagePickerController()
    
    var auth: FIRAuth!
    var storage: FIRStorage!
    var database: FIRDatabase!
    var authUI: FUIAuth!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Prepare UICollectionView
        self.collectionView.dataSource = self
        self.collectionView.delegate = self
        
        // Prepare nav bar
        self.title = "Firestories"
        self.navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Sign in", style: .plain, target: self, action: #selector(loginButtonPressed))
        
        self.takePhotoButton.addTarget(self, action: #selector(takePhotoButtonPressed), for: .touchUpInside)
        
        // Prepare imagepicker
        self.imagePicker.delegate = self
        
        // Initialize Firebase
        self.auth = FIRAuth.auth()
        self.storage = FIRStorage.storage()
        self.database = FIRDatabase.database()
        
        // Initialize FirebaseUI
        self.authUI = FUIAuth.defaultAuthUI()
        self.authUI.delegate = self
        self.authUI.providers = [ FUIGoogleAuth() ]
        
        // Handle auth state changes
        self.auth.addStateDidChangeListener { (auth, user) in
            if (user != nil) {
                self.navigationItem.leftBarButtonItem?.title = "Sign out"
                self.takePhotoButton.isEnabled = true
            } else {
                self.navigationItem.leftBarButtonItem?.title = "Sign in"
                self.takePhotoButton.isEnabled = false
            }
        }
        
        // Listen for new stories
        self.database.reference(withPath: "stories").queryLimited(toLast: 10).observe(.childAdded, with: { (snapshot) in
            self.displayStory(snapshot.value, snapshot.key)
        })
        
        // Listen to whenever a story changes
        self.database.reference(withPath: "stories").observe(.childChanged, with: { (snapshot) in
            self.displayStory(snapshot.value, snapshot.key)
        })
    }
    
    // MARK: UICollectionViewDataSource
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return self.storiesArray.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: reuseIdentifier, for: indexPath) as! StoryCollectionViewCell
        
        let story = self.storiesArray[indexPath.item]["value"] as! [String: String]
        
        var title = story["title"]
        if (story["emoji"] != nil && story["emoji"] != "") {
            title = story["emoji"]
        }
        cell.descriptionLabel.text = title
        
        self.storage.reference(forURL: story["downloadURL"]!).data(withMaxSize: 20 * 1024 * 1024) { (data, error) in
            if (error != nil) {
                print(error?.localizedDescription)
                return
            }
            cell.pictureImageView.image = UIImage(data: data!)
        }
        
        cell.emojiLabel.isHidden = true
        
        return cell
    }
    
    func displayStory(_ value: Any?, _ key: String!) {
        // Check if item exists, replace it if it does
        for (index, element) in self.storiesArray.enumerated() {
            if (element["key"] as! String == key) {
                self.storiesArray[index] = ["key": key, "value": value as Any]
                self.collectionView.reloadItems(at: [IndexPath(item: index, section: 0)])
                return
            }
        }
        // Otherwise, insert the item
        self.storiesArray.insert(["key": key, "value": value as Any], at: 0)
        self.collectionView.insertItems(at: [IndexPath(item: 0, section: 0)])
    }
    
    // MARK: UICollectionViewDelegateFlowLayout
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let paddingSpace = self.sectionInsets.left * (self.itemsPerRow + 1)
        let availableWidth = view.frame.width - paddingSpace
        let widthPerItem = availableWidth / itemsPerRow
        
        return CGSize(width: widthPerItem, height: widthPerItem)
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, insetForSectionAt section: Int) -> UIEdgeInsets {
        return self.sectionInsets
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumLineSpacingForSectionAt section: Int) -> CGFloat {
        return self.sectionInsets.left
    }
    
    // MARK: Handle login button pressed
    func loginButtonPressed() {
        if (self.auth.currentUser != nil) {
            try! self.auth.signOut()
        } else {
            // Start FirebaseUI login flow
            let loginViewController = self.authUI.authViewController()
            self.present(loginViewController, animated: true, completion: nil)
        }
    }
    
    // MARK: FUIAuthDelegate
    func authUI(_ authUI: FUIAuth, didSignInWith user: FIRUser?, error: Error?) {
        if (error != nil) {
            print(error?.localizedDescription)
            return
        }
    }
    
    // MARK: Handle photo picker
    func takePhotoButtonPressed() {
        self.imagePicker.allowsEditing = false
        self.imagePicker.sourceType = .photoLibrary
        
        self.present(self.imagePicker, animated: true, completion: nil)
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        
        guard let image: UIImage = info[UIImagePickerControllerOriginalImage] as? UIImage else { return }
        let imageData = UIImagePNGRepresentation(image)!
        let imageMetadata = FIRStorageMetadata()
        imageMetadata.contentType = "image/png"
        
        // Generate a storage reference
        let storageRef = self.storage.reference()
            .child((self.auth.currentUser?.uid)!)
            .child(NSUUID().uuidString)
        
        // Upload the image
        storageRef.put(imageData, metadata: imageMetadata) { (metadata, error) in
            if (error != nil) {
                print(error?.localizedDescription)
                return
            }
            
            //          // Add story locally
            //          let story = [
            //            "downloadURL": metadata?.downloadURL()!.absoluteString,
            //            "title": self.textInput.text,
            //          ] as [String : Any]
            //          self.displayStory(story, metadata?.name)
            
            // Generate a database reference
            let dbRef = self.database.reference(withPath: "stories").childByAutoId()
            
            // Write to the database
            dbRef.setValue([
                "filePath": metadata?.path!,
                "downloadURL": metadata?.downloadURL()!.absoluteString,
                "title": self.textInput.text,
                "uid": self.auth.currentUser?.uid
                ])
            
            self.textInput.text = ""
        }
        
        self.dismiss(animated: true)
    }
}
